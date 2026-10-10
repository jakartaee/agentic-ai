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
package ee.jakarta.examples.ai.agent.inquiryintake;

import jakarta.ai.agent.Action;
import jakarta.ai.agent.Agent;
import jakarta.ai.agent.Decision;
import jakarta.ai.agent.LargeLanguageModel;
import jakarta.ai.agent.Outcome;
import jakarta.ai.agent.Trigger;
import jakarta.inject.Inject;

import java.util.logging.Logger;

/**
 * Helps the customer write the message field of a contact form. Exercises
 * the four specification phases and the iterate-on-current-artifact pattern:
 * on a refine turn the whole current message is passed back into the prompt
 * so the model edits the real draft rather than relying on conversational
 * memory alone.
 */
@Agent(name = "AssistAgent",
       description = "Drafts and refines the message of a customer contact inquiry.")
public class AssistAgent {

    private static final Logger LOGGER = Logger.getLogger(AssistAgent.class.getName());

    @Inject
    LargeLanguageModel model;

    @Inject
    MessageStore store;

    @Trigger
    void onRequest(AssistRequest request) {
        LOGGER.info("[TRIGGER] " + (request.refine() ? "refine" : "draft") + " request");
    }

    @Decision
    boolean hasEnoughToProceed(AssistRequest request) {
        boolean proceed = request.refine()
                ? request.instruction() != null && !request.instruction().isBlank()
                : request.brief() != null && !request.brief().isBlank();
        LOGGER.info("[DECISION] proceed=" + proceed);
        return proceed;
    }

    @Action
    void compose(AssistRequest request) {
        String message;
        if (request.refine()) {
            LOGGER.info("[ACTION] refining message: " + request.instruction());
            message = model.query(
                    "Current message:\n{}\n\n"
                            + "Apply this change: {}\n\n"
                            + "Return only the revised message text, no commentary or framing.",
                    request.currentMessage(), request.instruction());
        } else {
            LOGGER.info("[ACTION] drafting message from brief");
            message = model.query(
                    "A customer is sending a contact inquiry. Turn this brief into a clear, "
                            + "concise, professional message (two to four sentences, first person). "
                            + "Return only the message text, no greeting, sign-off, or commentary.\n\n"
                            + "Brief: {}",
                    request.brief());
        }
        store.put(message.strip());
    }

    @Outcome
    void done(AssistRequest request) {
        LOGGER.info("[OUTCOME] message ready (" + store.get().length() + " chars)");
    }
}
