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
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.ObserverException;
import jakarta.inject.Inject;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * REST API for the contact-form assistant. Every endpoint that reaches the
 * LLM goes through {@link AssistAgent} by firing an {@link AssistRequest}
 * event; the resource never calls {@link jakarta.ai.agent.LargeLanguageModel}
 * directly. {@code Event.fire(...)} is synchronous, so the agent's result is
 * in {@link AssistResponse} by the time the method returns.
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
    AssistResponse response;

    @Inject
    Event<AssistRequest> trigger;

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
    public Response draft(DraftBody body) {
        String brief = body == null ? null : body.brief();
        return runFlow(new AssistRequest.Draft(brief),
                       () -> Map.of("message", response.message()));
    }

    /** Refine the current message with a chat instruction. */
    @POST
    @Path("message/refine")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response refine(RefineBody body) {
        String instruction = body == null ? null : body.instruction();
        return runFlow(new AssistRequest.Refine(messages.get(), instruction),
                       () -> Map.of("message", response.message()));
    }

    /** Classify the message into one of the configured inquiry types. */
    @POST
    @Path("classify")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response classify(ClassifyBody body) {
        String message = body == null ? null : body.message();
        return runFlow(new AssistRequest.Classify(message),
                       () -> Map.of("inquiryType", response.inquiryType()));
    }

    /** Pull the sender's name, email, and company out of a block of text. */
    @POST
    @Path("extract")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response extract(ExtractBody body) {
        String text = body == null ? null : body.text();
        return runFlow(new AssistRequest.Extract(text),
                       () -> {
                           JsonObject identity = response.identity();
                           return identity == null ? Map.of() : identity;
                       });
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

    private Response runFlow(AssistRequest request, Supplier<Object> successBody) {
        try {
            trigger.fire(request);
        } catch (ObserverException wrapper) {
            Throwable cause = wrapper.getCause();
            if (cause instanceof LLMException llm) {
                LOGGER.log(Level.WARNING, "LLM call failed", llm);
                return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                        .type(MediaType.TEXT_PLAIN)
                        .entity("The LLM backend is unavailable. Check that the provider "
                                + "configured in microprofile-config.properties is running.")
                        .build();
            }
            throw wrapper;
        }
        return Response.ok(successBody.get()).build();
    }

    public record DraftBody(String brief) {}

    public record RefineBody(String instruction) {}

    public record ClassifyBody(String message) {}

    public record ExtractBody(String text) {}
}
