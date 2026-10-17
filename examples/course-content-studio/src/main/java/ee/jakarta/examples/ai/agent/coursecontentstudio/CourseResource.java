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
package ee.jakarta.examples.ai.agent.coursecontentstudio;

import ee.jakarta.examples.ai.agent.coursecontentstudio.model.ChapterSubmitted;
import ee.jakarta.examples.ai.agent.coursecontentstudio.model.CoursePacket;
import ee.jakarta.examples.ai.agent.coursecontentstudio.model.LessonApproved;
import ee.jakarta.examples.ai.agent.coursecontentstudio.model.Section;
import ee.jakarta.examples.ai.agent.coursecontentstudio.model.SectionRefineRequested;
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

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * REST surface for the studio: generate, refine and approve each fire the
 * event of one agent and return what it produced.
 */
@Path("/")
@RequestScoped
public class CourseResource {

    private static final Logger LOGGER = Logger.getLogger(CourseResource.class.getName());

    @Inject
    Event<ChapterSubmitted> chapters;

    @Inject
    Event<SectionRefineRequested> refinements;

    @Inject
    Event<LessonApproved> approvals;

    @Inject
    PacketStore store;

    @Inject
    PublishedLessonStore lessons;

    @Inject
    PublishOutput publishOutput;

    @GET
    @Path("packet")
    @Produces(MediaType.APPLICATION_JSON)
    public Response packet() {
        return found(store.current(), "No packet yet.");
    }

    @POST
    @Path("packet/generate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response generate(@NotNull ChapterSubmitted chapter) {
        fire(() -> chapters.fire(chapter));
        return Response.ok(store.current()).build();
    }

    @POST
    @Path("packet/refine-section")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response refineSection(@NotNull RefineSectionRequest body) {
        CoursePacket base = store.current();
        if (base == null) {
            return error(Response.Status.BAD_REQUEST, "Nothing to refine yet — generate a packet first.");
        }
        fire(() -> refinements.fire(
                new SectionRefineRequested(base, body.section(), body.instruction())));
        return Response.ok(store.current()).build();
    }

    @POST
    @Path("packet/approve")
    @Produces(MediaType.APPLICATION_JSON)
    public Response approve() {
        CoursePacket packet = store.current();
        if (packet == null) {
            return error(Response.Status.BAD_REQUEST, "Nothing to approve yet — generate a packet first.");
        }
        fire(() -> approvals.fire(new LessonApproved(packet)));
        if (publishOutput.stopReason() != null) {
            return error(Response.Status.fromStatusCode(422), publishOutput.stopReason());
        }
        return Response.ok(lessons.current()).build();
    }

    @GET
    @Path("lesson")
    @Produces(MediaType.APPLICATION_JSON)
    public Response lesson() {
        return found(lessons.current(), "No lesson has been published yet.");
    }

    private static void fire(Runnable workflow) {
        try {
            workflow.run();
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

    public record RefineSectionRequest(Section section, String instruction) {
    }

    public record Message(String message) {
    }
}
