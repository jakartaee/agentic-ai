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

import jakarta.ai.agent.LLMException;
import jakarta.ai.agent.LargeLanguageModel;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.ObserverException;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.StringReader;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * REST API for the contact-form assistant. The {@code /message/*} endpoints
 * run the {@link AssistAgent} workflow via a CDI event (synchronous, so the
 * agent's result is in the {@link MessageStore} by the time the method
 * returns). The {@code /classify} and {@code /extract} endpoints are
 * one-shot calls that use the {@link LargeLanguageModel} facade directly —
 * no stateful workflow, no event — because those tasks are stateless utility
 * calls, not iterative artifact editing.
 */
@Path("")
@RequestScoped
public class AssistResource {

    private static final Logger LOGGER = Logger.getLogger(AssistResource.class.getName());

    @Inject
    InquiryFormSpec form;

    @Inject
    MessageStore messages;

    @Inject
    Event<AssistRequest> trigger;

    @Inject
    LargeLanguageModel model;

    /** The form schema; the page renders the fields from this. */
    @GET
    @Path("form")
    @Produces(MediaType.APPLICATION_JSON)
    public FormSpec form() {
        return form.spec();
    }

    /** The current message draft, or 204 before anything has been drafted. */
    @GET
    @Path("message")
    @Produces(MediaType.APPLICATION_JSON)
    public Response currentMessage() {
        String msg = messages.get();
        if (msg == null || msg.isBlank()) {
            return Response.noContent().build();
        }
        return Response.ok(Map.of("message", msg)).build();
    }

    /** Draft a polished message from a short customer brief. */
    @POST
    @Path("message/draft")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response draft(DraftRequest request) {
        String brief = request == null ? null : request.brief();
        return runWorkflow(new AssistRequest(brief, null, null));
    }

    /** Refine the current message with a chat instruction. */
    @POST
    @Path("message/refine")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response refine(RefineRequest request) {
        String instruction = request == null ? null : request.instruction();
        return runWorkflow(new AssistRequest(null, instruction, messages.get()));
    }

    /** Classify a message into one of the inquiry types. One-shot LLM call, no agent. */
    @POST
    @Path("classify")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response classify(ClassifyRequest request) {
        if (request == null || request.message() == null || request.message().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .type(MediaType.TEXT_PLAIN)
                    .entity("A message is required.").build();
        }
        try {
            String raw = model.query(
                    "Classify this customer inquiry into exactly one of: {}. "
                            + "Return only the single word, nothing else.\n\n{}",
                    InquiryFormSpec.INQUIRY_TYPES, request.message());
            String type = raw.strip().replaceAll("[^A-Za-z]", "");
            if (!InquiryFormSpec.INQUIRY_TYPES.contains(type)) {
                type = "General";
            }
            return Response.ok(Map.of("inquiryType", type)).build();
        } catch (LLMException e) {
            return llmUnavailable(e);
        }
    }

    /**
     * Pull the sender's name, email, and company out of a block of text. Used
     * when the customer pastes a longer message that already identifies them
     * ("Hi, I'm Jane Smith from Acme, jane@acme.com..."). One-shot LLM call,
     * no agent; missing values come back as empty strings.
     */
    @POST
    @Path("extract")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response extract(ExtractRequest request) {
        if (request == null || request.text() == null || request.text().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .type(MediaType.TEXT_PLAIN)
                    .entity("Some text is required.").build();
        }
        try {
            String raw = model.query(
                    "Extract the sender's identity from the text below. Return a JSON object with "
                            + "exactly these keys: \"name\", \"businessEmail\", \"company\". If a value "
                            + "is not clearly present, use an empty string for that key. Return only the "
                            + "JSON object, with no markdown or commentary.\n\n{}",
                    request.text());
            String json = stripCodeFences(raw);
            JsonObject extracted;
            try (JsonReader reader = Json.createReader(new StringReader(json))) {
                extracted = reader.readObject();
            } catch (RuntimeException parseFailure) {
                throw new LLMException("Extraction returned non-JSON output", parseFailure);
            }
            return Response.ok(extracted.toString()).build();
        } catch (LLMException e) {
            return llmUnavailable(e);
        }
    }

    /**
     * Hand the completed inquiry off. In a real deployment this is where the
     * record would reach a CRM, a ticketing system, or an outbound email; here
     * it just logs. The full submission comes from the browser so no part of
     * it needs to be reconstructed on the server.
     */
    @POST
    @Path("submit")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response submit(JsonObject submission) {
        if (submission == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .type(MediaType.TEXT_PLAIN)
                    .entity("A submission is required.").build();
        }
        LOGGER.info("[SUBMIT] inquiry received: " + submission);
        messages.put("");
        return Response.ok(Map.of("status", "submitted")).build();
    }

    private Response runWorkflow(AssistRequest request) {
        try {
            trigger.fire(request);
        } catch (ObserverException wrapper) {
            Throwable cause = wrapper.getCause();
            if (cause instanceof LLMException llm) {
                return llmUnavailable(llm);
            }
            throw wrapper;
        }
        return Response.ok(Map.of("message", messages.get())).build();
    }

    private Response llmUnavailable(LLMException e) {
        LOGGER.log(Level.WARNING, "LLM call failed", e);
        return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .type(MediaType.TEXT_PLAIN)
                .entity("The LLM backend is unavailable. Check that the provider "
                        + "configured in microprofile-config.properties is running.")
                .build();
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

    public record DraftRequest(String brief) {}

    public record RefineRequest(String instruction) {}

    public record ClassifyRequest(String message) {}

    public record ExtractRequest(String text) {}
}
