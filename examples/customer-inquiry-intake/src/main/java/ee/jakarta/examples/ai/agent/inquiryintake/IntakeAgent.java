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
import jakarta.ai.agent.Result;
import jakarta.ai.agent.Trigger;
import jakarta.inject.Inject;
import jakarta.validation.Valid;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Processes a raw contact-form submission into a reviewed inquiry record.
 * The agent uses several API features that extend what Example 1 shows:
 *
 * <ul>
 *   <li>{@code @Valid} on the trigger parameter — Bean Validation on
 *       {@link IntakeRequest} runs before the workflow body.</li>
 *   <li>{@code @Decision} returning a {@link Result} so the reason the
 *       workflow stopped is carried as the Result's {@code details}, not
 *       just a boolean.</li>
 *   <li>A second {@code @Decision} that gates mid-pipeline — ordered
 *       after {@link #enrichIdentity} by taking an {@link Identity} as a
 *       parameter.</li>
 *   <li>Three {@code @Action} methods ordered by parameter-type
 *       dataflow (no explicit {@code order=}). {@link #enrichIdentity} and
 *       {@link #classify} both take only {@link IntakeRequest} and can run
 *       first; {@link #assemble} declares both of their returned records
 *       as parameters, so the runtime runs it after.</li>
 *   <li>Typed {@code model.query(Class, ...)} in {@link #enrichIdentity}
 *       — the LLM response binds directly to {@link Identity} via
 *       JSON-B, with no hand-rolled JSON parsing.</li>
 *   <li>{@code @HandleException} catching {@link LLMException} with a
 *       fallback record, so the UI always has something to show.</li>
 * </ul>
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
    void onSubmit(@Valid IntakeRequest request) {
        LOGGER.info("[TRIGGER] inquiry received");
    }

    @Decision
    Result canProcess(IntakeRequest request) {
        String message = request.message().strip();
        if (message.length() < 20) {
            Result stop = new Result(false, "Message is too short to process meaningfully.");
            LOGGER.info("[DECISION canProcess] stop: " + stop.details());
            return stop;
        }
        LOGGER.info("[DECISION canProcess] proceed");
        return new Result(true, null);
    }

    @Action
    Identity enrichIdentity(IntakeRequest request) {
        if (notBlank(request.name()) && notBlank(request.email()) && notBlank(request.company())) {
            LOGGER.info("[ACTION enrichIdentity] all fields present, skipping LLM");
            return new Identity(request.name(), request.email(), request.company());
        }
        LOGGER.info("[ACTION enrichIdentity] filling gaps from the message");
        Identity extracted = model.query(
                "Extract the sender's identity from the text below. Return a JSON object with "
                        + "exactly these keys: \"name\", \"email\", \"company\". Use empty strings "
                        + "for anything not clearly present. Return only the JSON object, no "
                        + "markdown or commentary.\n\n{}",
                Identity.class,
                request.message());
        return new Identity(
                preferGiven(request.name(),    extracted.name()),
                preferGiven(request.email(),   extracted.email()),
                preferGiven(request.company(), extracted.company()));
    }

    @Action
    Classification classify(IntakeRequest request) {
        LOGGER.info("[ACTION classify]");
        String raw = model.query(
                "Classify this customer inquiry into exactly one of: {}. Return only the single word.\n\n{}",
                List.of(Classification.values()), request.message());
        try {
            return Classification.valueOf(raw.strip().replaceAll("[^A-Za-z]", "").toUpperCase());
        } catch (IllegalArgumentException notInEnum) {
            return Classification.GENERAL;
        }
    }

    @Decision
    boolean hasEnoughContent(Identity identity, IntakeRequest request) {
        boolean anyIdentity = notBlank(identity.name())
                || notBlank(identity.email())
                || notBlank(identity.company());
        boolean substantive = request.message().strip().split("\\s+").length >= 6;
        boolean proceed = anyIdentity || substantive;
        LOGGER.info("[DECISION hasEnoughContent] proceed=" + proceed
                + " (anyIdentity=" + anyIdentity + ", substantive=" + substantive + ")");
        return proceed;
    }

    @Action
    IntakeRecord assemble(IntakeRequest request, Identity identity, Classification classification) {
        LOGGER.info("[ACTION assemble] (" + classification + ")");
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
}
