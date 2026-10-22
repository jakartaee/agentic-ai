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
package ee.jakarta.examples.ai.agent.ask;

import jakarta.ai.agent.LLMException;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.ObserverException;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Fires the {@link Question} event that triggers the agent and returns the
 * answer it stored.
 */
@Path("ask")
@RequestScoped
public class AskResource {

    private static final Logger LOGGER = Logger.getLogger(AskResource.class.getName());

    @Inject
    private Event<Question> trigger;

    @Inject
    private AnswerStore answers;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response ask(@NotNull AskRequest request) {
        String text = request.question();

        try {
            trigger.fire(new Question(text));
        } catch (ObserverException wrapper) {
            Throwable cause = wrapper.getCause();
            if (cause instanceof ConstraintViolationException) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new Message("A question is required."))
                        .build();
            }
            if (cause instanceof LLMException) {
                LOGGER.log(Level.WARNING, "LLM call failed", cause);
                return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                        .entity(new Message("The LLM backend is unavailable. Check that the provider "
                                + "configured in microprofile-config.properties is running."))
                        .build();
            }
            throw wrapper;
        }

        return Response.ok(new AskResponse(text, answers.get(text))).build();
    }

    public record AskRequest(String question) {
    }

    public record AskResponse(String question, String answer) {
    }

    public record Message(String message) {
    }
}
