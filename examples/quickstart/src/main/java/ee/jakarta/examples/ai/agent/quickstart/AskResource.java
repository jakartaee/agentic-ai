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
package ee.jakarta.examples.ai.agent.quickstart;

import jakarta.ai.agent.LLMException;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
 * Fires the {@link Question} CDI event that triggers the agent workflow.
 * <p>
 * {@code Event.fire(...)} is synchronous, so the whole workflow (including the
 * LLM call) completes before it returns; the answer is then read back from the
 * {@link AnswerStore} and returned in the same HTTP response.
 * <p>
 * The request is validated declaratively: {@code @NotBlank} on the request
 * record and {@code @Valid} on the parameter, so a missing or empty question is
 * rejected with a 400 before this method body runs.
 */
@Path("ask")
@RequestScoped
public class AskResource {

    private static final Logger LOGGER = Logger.getLogger(AskResource.class.getName());

    @Inject
    Event<Question> trigger;

    @Inject
    AnswerStore answers;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response ask(@NotNull @Valid AskRequest request) {
        String text = request.question();

        try {
            trigger.fire(new Question(text));   // runs the entire workflow synchronously
        } catch (LLMException e) {
            // Event.fire is synchronous, so a model failure surfaces here rather than
            // in the agent. The most common cause is the configured backend not running.
            LOGGER.log(Level.WARNING, "LLM call failed", e);
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(new AskResponse(text,
                            "The LLM backend is unavailable. Check that the provider "
                                    + "configured in microprofile-config.properties is running."))
                    .build();
        }

        return Response.ok(new AskResponse(text, answers.get(text))).build();
    }

    public record AskRequest(@NotBlank String question) {
    }

    public record AskResponse(String question, String answer) {
    }
}
