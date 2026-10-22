/*****************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package ee.jakarta.examples.ai.agent.lessonbuilder;

import ee.jakarta.examples.ai.agent.lessonbuilder.model.CatalogEntry;
import ee.jakarta.examples.ai.agent.lessonbuilder.model.Lesson;
import ee.jakarta.examples.ai.agent.lessonbuilder.model.LessonPublished;
import ee.jakarta.examples.ai.agent.lessonbuilder.model.Topic;
import jakarta.ai.agent.LLMException;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.ObserverException;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.ServiceUnavailableException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * REST surface for the lesson builder: generate fires the lesson agent and
 * returns the draft; publish stores the edited lesson, fires the catalog
 * agent and returns the lesson with its catalog entry (if one was filed) or
 * the reason the agent declined to file it.
 */
@Path("/")
@RequestScoped
public class BuilderResource {

    private static final Logger LOGGER = Logger.getLogger(BuilderResource.class.getName());

    @Inject
    Event<Topic> topics;

    @Inject
    Event<LessonPublished> publications;

    @Inject
    DraftStore drafts;

    @Inject
    PublishedLessonStore lessons;

    @Inject
    Catalog catalog;

    @Inject
    CatalogOutput catalogOutput;

    @GET
    @Path("draft")
    @Produces(MediaType.APPLICATION_JSON)
    public Response draft() {
        return found(drafts.current(), "No draft yet.");
    }

    @POST
    @Path("draft/generate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response generate(@NotNull Topic topic) {
        try {
            topics.fire(topic);
        } catch (ObserverException wrapper) {
            Throwable cause = wrapper.getCause();
            if (cause instanceof ConstraintViolationException violations) {
                throw new BadRequestException(error(Response.Status.BAD_REQUEST, describe(violations)));
            }
            if (cause instanceof LLMException) {
                LOGGER.log(Level.WARNING, "LLM call failed", cause);
                throw new ServiceUnavailableException(error(Response.Status.SERVICE_UNAVAILABLE,
                        "The LLM backend failed: " + cause.getMessage()));
            }
            throw wrapper;
        }
        return Response.ok(drafts.current()).build();
    }

    @POST
    @Path("lesson/publish")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response publish(@NotNull Lesson lesson) {
        lessons.publish(lesson);
        try {
            publications.fire(new LessonPublished(lesson));
        } catch (ObserverException wrapper) {
            if (!(wrapper.getCause() instanceof LLMException)) {
                throw wrapper;
            }
            LOGGER.log(Level.WARNING, "cataloging failed", wrapper.getCause());
            catalogOutput.setStopReason("Published, but not cataloged: the LLM backend failed.");
        }
        return Response.ok(new Published(lesson, catalogOutput.filed(), catalogOutput.stopReason()))
                .build();
    }

    @GET
    @Path("lesson")
    @Produces(MediaType.APPLICATION_JSON)
    public Response lesson() {
        return found(lessons.current(), "No lesson has been published yet.");
    }

    @GET
    @Path("catalog")
    @Produces(MediaType.APPLICATION_JSON)
    public List<CatalogEntry> catalog() {
        return catalog.entries();
    }

    private static String describe(ConstraintViolationException e) {
        return e.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
    }

    private static Response found(Object entity, String otherwise) {
        return entity == null ? error(Response.Status.NOT_FOUND, otherwise) : Response.ok(entity).build();
    }

    private static Response error(Response.StatusType status, String message) {
        return Response.status(status).entity(new Message(message)).build();
    }

    public record Published(Lesson lesson, CatalogEntry filed, String notice) {
    }

    public record Message(String message) {
    }
}
