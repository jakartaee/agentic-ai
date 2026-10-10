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
import jakarta.ai.agent.HandleException;
import jakarta.ai.agent.LLMException;
import jakarta.ai.agent.LargeLanguageModel;
import jakarta.ai.agent.Outcome;
import jakarta.ai.agent.Trigger;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

import java.io.StringReader;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Processes a raw contact-form submission into a reviewed inquiry record.
 * Three actions run as a pipeline — their order is set by the data each one
 * needs, not by an explicit {@code order=} on the annotations:
 *
 * <ol>
 *   <li>{@link #enrichIdentity} and {@link #classify} both take only the
 *       {@link IntakeRequest} and can run first.</li>
 *   <li>{@link #assemble} takes an {@link Identity} and a
 *       {@link Classification}, so the runtime runs it after the two above
 *       have produced their results.</li>
 * </ol>
 *
 * {@link #onLlmFailure} catches an {@link LLMException} from any of the
 * actions and writes a minimal fallback record, so the customer sees
 * "received, we'll process manually" instead of a 500 when the model is
 * unavailable.
 */
@Agent(name = "IntakeAgent",
       description = "Processes a customer contact-form submission into a reviewed inquiry record.")
public class IntakeAgent {

    private static final Logger LOGGER = Logger.getLogger(IntakeAgent.class.getName());

    @Inject
    LargeLanguageModel model;

    @Inject
    IntakeOutput output;

    @Trigger
    void onSubmit(IntakeRequest request) {
        LOGGER.info("[TRIGGER] inquiry received");
    }

    @Decision
    boolean canProcess(IntakeRequest request) {
        boolean proceed = notBlank(request.message());
        LOGGER.info("[DECISION] proceed=" + proceed);
        return proceed;
    }

    @Action
    Identity enrichIdentity(IntakeRequest request) {
        if (notBlank(request.name()) && notBlank(request.email()) && notBlank(request.company())) {
            LOGGER.info("[ACTION] enrichIdentity: all fields present, skipping LLM");
            return new Identity(request.name(), request.email(), request.company());
        }
        LOGGER.info("[ACTION] enrichIdentity: filling gaps from the message");
        JsonObject extracted = parseObject(model.query(
                "Extract the sender's identity from the text below. Return a JSON object with "
                        + "exactly these keys: \"name\", \"email\", \"company\". Use empty strings "
                        + "for anything not clearly present. Return only the JSON object.\n\n{}",
                request.message()));
        return new Identity(
                preferGiven(request.name(),    extracted.getString("name",    "")),
                preferGiven(request.email(),   extracted.getString("email",   "")),
                preferGiven(request.company(), extracted.getString("company", "")));
    }

    @Action
    Classification classify(IntakeRequest request) {
        LOGGER.info("[ACTION] classify");
        String raw = model.query(
                "Classify this customer inquiry into exactly one of: {}. Return only the single word.\n\n{}",
                List.of(Classification.values()), request.message());
        try {
            return Classification.valueOf(raw.strip().replaceAll("[^A-Za-z]", "").toUpperCase());
        } catch (IllegalArgumentException notInEnum) {
            return Classification.GENERAL;
        }
    }

    @Action
    IntakeRecord assemble(IntakeRequest request, Identity identity, Classification classification) {
        LOGGER.info("[ACTION] assemble (" + classification + ")");
        String polished = model.query(
                "Rewrite the following customer inquiry to be clear, concise, and professional. "
                        + "The inquiry is categorised as {}, so adopt that register. Keep the "
                        + "sender's voice and intent. Return only the revised message text.\n\n{}",
                classification, request.message()).strip();
        IntakeRecord record = new IntakeRecord(identity, classification, polished);
        output.setRecord(record);
        return record;
    }

    @Outcome
    void done(IntakeRequest request) {
        LOGGER.info("[OUTCOME] inquiry prepared");
    }

    @HandleException
    void onLlmFailure(LLMException failure, IntakeRequest request) {
        LOGGER.log(Level.WARNING, "LLM call failed; writing fallback record", failure);
        output.setRecord(IntakeRecord.fallbackFor(request));
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String preferGiven(String given, String extracted) {
        return notBlank(given) ? given : (extracted == null ? "" : extracted);
    }

    private JsonObject parseObject(String raw) {
        String json = stripCodeFences(raw);
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            return reader.readObject();
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
