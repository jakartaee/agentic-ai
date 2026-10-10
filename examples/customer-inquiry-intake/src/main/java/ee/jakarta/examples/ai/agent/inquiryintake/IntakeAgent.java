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
import jakarta.ai.agent.LLMException;
import jakarta.ai.agent.LargeLanguageModel;
import jakarta.ai.agent.Outcome;
import jakarta.ai.agent.Trigger;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonReader;
import jakarta.validation.Valid;

import java.io.StringReader;
import java.util.logging.Logger;

/**
 * Turns a raw customer inquiry into a structured record a team can act on,
 * and refines any part of that record on request. Exercises the four
 * specification phases and the iterate-on-current-artifact pattern: on a
 * refine turn the whole current inquiry is passed back into the prompt so
 * the model edits the real record rather than relying on conversational
 * memory alone.
 */
@Agent(name = "IntakeAgent",
       description = "Triages a free-text customer inquiry into a structured, actionable record.")
public class IntakeAgent {

    private static final Logger LOGGER = Logger.getLogger(IntakeAgent.class.getName());

    @Inject
    LargeLanguageModel model;

    @Inject
    InquiryStore store;

    @Trigger
    void onRequest(@Valid IntakeRequest request) {
        LOGGER.info("[TRIGGER] " + (request.refine() ? "refine" : "draft") + " request");
    }

    @Decision
    boolean hasEnoughToProceed(IntakeRequest request) {
        boolean proceed = request.refine()
                ? request.instruction() != null && !request.instruction().isBlank()
                : request.brief() != null && !request.brief().isBlank();
        LOGGER.info("[DECISION] proceed=" + proceed);
        return proceed;
    }

    @Action
    void render(IntakeRequest request) {
        String content;
        if (request.refine()) {
            LOGGER.info("[ACTION] refining inquiry: " + request.instruction());
            content = model.query(
                    "Current triaged inquiry JSON:\n{}\n\n"
                            + "Apply this change: {}\n\n"
                            + "Return the complete updated JSON object, keeping the same keys.",
                    request.currentInquiry(), request.instruction());
        } else {
            LOGGER.info("[ACTION] drafting a fresh inquiry from the brief");
            content = model.query(
                    "Triage this customer inquiry into a single JSON object. "
                            + "Use these exact keys: {}. "
                            + "Choose `category` from {} and `urgency` from {}. "
                            + "The raw inquiry follows:\n\n{}",
                    fieldNames(request.formSpec()),
                    optionsFor(request.formSpec(), "category"),
                    optionsFor(request.formSpec(), "urgency"),
                    request.brief());
        }
        String json = stripCodeFences(content);
        requireJsonObject(json);
        store.put(json);
    }

    @Outcome
    void complete(IntakeRequest request) {
        LOGGER.info("[OUTCOME] inquiry ready (" + store.get().length() + " chars)");
    }

    private static String fieldNames(FormSpec spec) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (FieldSpec f : spec.fields()) {
            if (!first) sb.append(", ");
            sb.append('"').append(f.name()).append('"');
            first = false;
        }
        return sb.append(']').toString();
    }

    private static String optionsFor(FormSpec spec, String fieldName) {
        for (FieldSpec f : spec.fields()) {
            if (fieldName.equals(f.name())) {
                return f.options().toString();
            }
        }
        return "[]";
    }

    private static void requireJsonObject(String content) {
        try (JsonReader reader = Json.createReader(new StringReader(content))) {
            reader.readObject();
        } catch (RuntimeException parseFailure) {
            throw new LLMException("LLM returned output that is not a JSON object", parseFailure);
        }
    }

    /** LLMs sometimes wrap output in ```json ... ``` despite instructions; strip it. */
    private static String stripCodeFences(String content) {
        if (content == null) {
            return "";
        }
        String trimmed = content.strip();
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
