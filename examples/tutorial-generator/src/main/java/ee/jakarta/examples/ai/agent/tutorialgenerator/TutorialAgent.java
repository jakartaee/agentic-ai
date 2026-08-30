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
package ee.jakarta.examples.ai.agent.tutorialgenerator;

import jakarta.ai.agent.Action;
import jakarta.ai.agent.Agent;
import jakarta.ai.agent.Decision;
import jakarta.ai.agent.LargeLanguageModel;
import jakarta.ai.agent.Outcome;
import jakarta.ai.agent.Result;
import jakarta.ai.agent.Trigger;
import jakarta.inject.Inject;

import java.util.logging.Logger;

/**
 * Generates and refines an HTML tutorial that explains a web form, field by
 * field, using the configured LLM. Exercises the four specification phases and
 * supports a chat refinement loop: when the request carries the current HTML,
 * the {@code @Action} revises it instead of regenerating from scratch.
 */
@Agent(name = "TutorialAgent", description = "Generates and refines an HTML tutorial explaining a web form.")
public class TutorialAgent {

    private static final Logger LOGGER = Logger.getLogger(TutorialAgent.class.getName());

    @Inject
    LargeLanguageModel model;

    @Inject
    TutorialStore store;

    @Trigger
    void onRequest(TutorialRequest request) {
        boolean refine = request.currentHtml() != null && !request.currentHtml().isBlank();
        LOGGER.info("[TRIGGER] tutorial request (" + (refine ? "refine" : "generate") + ")");
    }

    @Decision
    Result hasFields(TutorialRequest request) {
        boolean proceed = request.formSpec() != null && !request.formSpec().fields().isEmpty();
        LOGGER.info("[DECISION] proceed=" + proceed);
        return new Result(proceed, request);
    }

    @Action
    void render(TutorialRequest request) {
        String content;
        if (request.currentHtml() == null || request.currentHtml().isBlank()) {
            LOGGER.info("[ACTION] generating field-guide JSON from the form spec...");
            content = model.query(
                    "Generate the field-guide JSON for this form. "
                            + "Use each field's name attribute as the JSON key: {}", request.formSpec());
        } else {
            LOGGER.info("[ACTION] refining field-guide: " + request.instruction());
            content = model.query(
                    "Current field-guide JSON:\n{}\n\n"
                            + "Apply this change to the explanations: {}\n\n"
                            + "Return the complete updated JSON object.",
                    request.currentHtml(), request.instruction());
        }
        store.put(stripCodeFences(content));
    }

    @Outcome
    void complete(TutorialRequest request) {
        LOGGER.info("[OUTCOME] tutorial ready (" + store.get().length() + " chars)");
    }

    /** LLMs sometimes wrap output in ```html ... ``` despite instructions; strip it. */
    private static String stripCodeFences(String html) {
        if (html == null) {
            return "";
        }
        String trimmed = html.strip();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline > 0) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
        }
        return trimmed.strip();
    }
}
