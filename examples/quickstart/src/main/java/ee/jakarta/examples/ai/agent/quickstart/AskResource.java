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

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Fires the {@link Question} CDI event that triggers the agent workflow.
 * <p>
 * {@code Event.fire(...)} is synchronous, so the whole workflow (including the
 * LLM call) completes before it returns; the answer is then read back from the
 * {@link AnswerStore} and returned in the same HTTP response.
 */
@Path("ask")
@RequestScoped
public class AskResource {

    @Inject
    Event<Question> trigger;

    @Inject
    AnswerStore answers;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response ask(AskRequest request) {
        String text = request == null || request.question() == null ? "" : request.question();
        Question question = new Question(text);

        trigger.fire(question);   // runs the entire workflow synchronously

        String answer = answers.get(text);
        return Response.ok(new AskResponse(
                text,
                answer != null ? answer : "(no answer — workflow terminated, or LLM provider is 'none')"
        )).build();
    }

    public record AskRequest(String question) {
    }

    public record AskResponse(String question, String answer) {
    }
}
