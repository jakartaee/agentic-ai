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
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

import java.io.StringReader;
import java.util.logging.Logger;

/**
 * The one place that talks to the LLM for this sample. Handles four flows,
 * selected by the concrete subtype of the {@link AssistRequest} event:
 * drafting the customer's message from a brief, refining the current
 * message, classifying the message into one of the configured inquiry
 * types, and extracting the sender's identity from a block of text. The
 * iterate-on-current-artifact pattern lives in the refine flow: the whole
 * current message is passed back into the prompt on every turn so the model
 * edits the real draft rather than relying on conversational memory alone.
 */
@Agent(name = "AssistAgent",
       description = "Drafts, refines, classifies and extracts for a customer contact form.")
public class AssistAgent {

    private static final Logger LOGGER = Logger.getLogger(AssistAgent.class.getName());

    @Inject
    LargeLanguageModel model;

    @Inject
    MessageStore messages;

    @Inject
    AssistResponse response;

    @Trigger
    void onRequest(AssistRequest request) {
        LOGGER.info("[TRIGGER] " + flowName(request));
    }

    @Decision
    boolean hasEnoughToProceed(AssistRequest request) {
        boolean proceed;
        if (request instanceof AssistRequest.Draft d) {
            proceed = notBlank(d.brief());
        } else if (request instanceof AssistRequest.Refine r) {
            proceed = notBlank(r.currentMessage()) && notBlank(r.instruction());
        } else if (request instanceof AssistRequest.Classify c) {
            proceed = notBlank(c.message());
        } else if (request instanceof AssistRequest.Extract e) {
            proceed = notBlank(e.text());
        } else {
            proceed = false;
        }
        LOGGER.info("[DECISION] proceed=" + proceed);
        return proceed;
    }

    @Action
    void dispatch(AssistRequest request) {
        if (request instanceof AssistRequest.Draft d) {
            draftMessage(d);
        } else if (request instanceof AssistRequest.Refine r) {
            refineMessage(r);
        } else if (request instanceof AssistRequest.Classify c) {
            classifyInquiry(c);
        } else if (request instanceof AssistRequest.Extract e) {
            extractIdentity(e);
        }
    }

    @Outcome
    void done(AssistRequest request) {
        LOGGER.info("[OUTCOME] " + flowName(request) + " ready");
    }

    private void draftMessage(AssistRequest.Draft request) {
        LOGGER.info("[ACTION] drafting message from brief");
        String msg = model.query(
                "A customer is sending a contact inquiry. Turn this brief into a clear, "
                        + "concise, professional message (two to four sentences, first person). "
                        + "Return only the message text, no greeting, sign-off, or commentary.\n\n"
                        + "Brief: {}",
                request.brief()).strip();
        messages.put(msg);
        response.setMessage(msg);
    }

    private void refineMessage(AssistRequest.Refine request) {
        LOGGER.info("[ACTION] refining message: " + request.instruction());
        String msg = model.query(
                "Current message:\n{}\n\n"
                        + "Apply this change: {}\n\n"
                        + "Return only the revised message text, no commentary or framing.",
                request.currentMessage(), request.instruction()).strip();
        messages.put(msg);
        response.setMessage(msg);
    }

    private void classifyInquiry(AssistRequest.Classify request) {
        LOGGER.info("[ACTION] classifying message");
        String raw = model.query(
                "Classify this customer inquiry into exactly one of: {}. "
                        + "Return only the single word, nothing else.\n\n{}",
                InquiryFormSpec.INQUIRY_TYPES, request.message());
        String type = raw.strip().replaceAll("[^A-Za-z]", "");
        if (!InquiryFormSpec.INQUIRY_TYPES.contains(type)) {
            type = "General";
        }
        response.setInquiryType(type);
    }

    private void extractIdentity(AssistRequest.Extract request) {
        LOGGER.info("[ACTION] extracting identity");
        String raw = model.query(
                "Extract the sender's identity from the text below. Return a JSON object with "
                        + "exactly these keys: \"name\", \"businessEmail\", \"company\". If a value "
                        + "is not clearly present, use an empty string for that key. Return only the "
                        + "JSON object, with no markdown or commentary.\n\n{}",
                request.text());
        String json = stripCodeFences(raw);
        JsonObject identity;
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            identity = reader.readObject();
        } catch (RuntimeException parseFailure) {
            throw new LLMException("Extraction returned non-JSON output", parseFailure);
        }
        response.setIdentity(identity);
    }

    private static String flowName(AssistRequest request) {
        return request.getClass().getSimpleName().toLowerCase();
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
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
