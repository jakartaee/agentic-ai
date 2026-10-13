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
import jakarta.ai.agent.OnException;
import jakarta.ai.agent.LLMException;
import jakarta.ai.agent.LargeLanguageModel;
import jakarta.ai.agent.Outcome;
import jakarta.ai.agent.Trigger;
import jakarta.inject.Inject;
import jakarta.validation.Valid;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Processes a raw contact-form submission into a reviewed inquiry record.
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
        LOGGER.info("[TRIGGER] inquiry received: " + request);
    }

    @Decision
    boolean looksLikeRealInquiry(IntakeRequest request) {
        if (looksLikePlaceholderText(request.message())) {
            output.setStopReason("This looks like a test or placeholder submission. Please send your actual inquiry.");
            LOGGER.info("[DECISION looksLikeRealInquiry] stop: placeholder/test submission");
            return false;
        }
        LOGGER.info("[DECISION looksLikeRealInquiry] proceed");
        return true;
    }

    @Action
    Identity enrichIdentity(IntakeRequest request) {
        LOGGER.info("[ACTION enrichIdentity] extracting identity and inferring company from email domain");
        Identity extracted = model.query(
                "Extract the sender's identity from the inquiry below. Return a JSON object "
                        + "with exactly these keys: \"name\", \"email\", \"company\". For "
                        + "\"name\" and \"email\", pull them from the message text (empty "
                        + "string if not clearly present). For \"company\", infer it from the "
                        + "business email domain — map the domain to the real company name "
                        + "(e.g. \"acme.com\" -> \"Acme\"). If the email is from a free "
                        + "consumer provider (gmail, yahoo, outlook, hotmail, icloud, proton) "
                        + "or no email is available, return an empty string for company. "
                        + "Return only the JSON object, no markdown or commentary.\n\n"
                        + "Already-provided name: {}\nAlready-provided email: {}\n\n"
                        + "Message:\n{}",
                Identity.class,
                request.name() == null ? "" : request.name(),
                request.email() == null ? "" : request.email(),
                request.message());
        return new Identity(
                preferGiven(request.name(),  extracted.name()),
                preferGiven(request.email(), extracted.email()),
                extracted.company() == null ? "" : extracted.company());
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

    @Action
    Title draftTitle(IntakeRequest request) {
        LOGGER.info("[ACTION draftTitle]");
        String raw = model.query(
                "Write a short subject line for this customer inquiry, suitable for a "
                        + "ticketing system. Keep it under 60 characters, focus on the main "
                        + "ask, and use title case. Return only the subject line text — no "
                        + "quotes, no prefix, no trailing punctuation.\n\n{}",
                request.message()).strip();
        if (raw.length() > 80) {
            raw = raw.substring(0, 77) + "...";
        }
        return new Title(raw);
    }

    @Decision
    boolean hasEnoughContent(Identity identity, IntakeRequest request) {
        boolean anyIdentity = notBlank(identity.name()) || notBlank(identity.email());
        boolean substantive = request.message().strip().split("\\s+").length >= 6;
        boolean proceed = anyIdentity || substantive;
        LOGGER.info("[DECISION hasEnoughContent] proceed=" + proceed
                + " (anyIdentity=" + anyIdentity + ", substantive=" + substantive + ")");
        if (!proceed) {
            output.setStopReason("We could not route this inquiry. Please add your name or email, "
                    + "or elaborate on what you are asking for.");
        }
        return proceed;
    }

    @Action
    PolishedMessage polish(IntakeRequest request, Classification classification) {
        LOGGER.info("[ACTION polish] (" + classification + ")");
        String text = model.query(
                "Rewrite the following customer inquiry to be clear, concise, and professional. "
                        + "The inquiry is categorised as {}, so adopt that register. Keep the "
                        + "sender's voice and intent. Return only the revised message text.\n\n{}",
                classification, request.message()).strip();
        return new PolishedMessage(text);
    }

    @Outcome
    void done(Identity identity, Classification classification, Title title, PolishedMessage polished) {
        output.setRecord(new IntakeRecord(identity, classification, title.value(), polished.value()));
        LOGGER.info("[OUTCOME] inquiry prepared");
    }

    @OnException
    void onLlmFailure(LLMException failure, IntakeRequest request) {
        LOGGER.log(Level.WARNING, "LLM call failed; writing fallback record", failure);
        output.setRecord(IntakeRecord.fallbackFor(request));
    }

    private static final Set<String> PLACEHOLDER_TOKENS = Set.of(
            "test", "asdf", "asdfg", "qwerty", "lorem", "ipsum", "foo", "bar", "baz");

    private static boolean looksLikePlaceholderText(String message) {
        String lower = message.toLowerCase();
        if (lower.contains("lorem ipsum")) {
            return true;
        }
        String[] words = lower.split("\\W+");
        if (words.length == 0) {
            return false;
        }
        long placeholders = Arrays.stream(words).filter(PLACEHOLDER_TOKENS::contains).count();
        return placeholders >= 2 && placeholders * 2 >= words.length;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String preferGiven(String given, String extracted) {
        return notBlank(given) ? given : (extracted == null ? "" : extracted);
    }
}
