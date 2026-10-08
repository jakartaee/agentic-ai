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

import jakarta.ai.agent.LLMException;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.ObserverException;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * REST API for the tutorial UI. Firing the {@link TutorialRequest} event is
 * synchronous, so the agent workflow (including the LLM call) completes before
 * the method returns and the freshly produced guide can be read from the
 * {@link TutorialStore}.
 */
@Path("")
@RequestScoped
public class TutorialResource {

    private static final Logger LOGGER = Logger.getLogger(TutorialResource.class.getName());

    @Inject
    Event<TutorialRequest> trigger;

    @Inject
    TutorialStore store;

    @Inject
    CustomerFormSpec form;

    /** The form metadata; the page renders the live form from this. */
    @GET
    @Path("form")
    @Produces(MediaType.APPLICATION_JSON)
    public FormSpec form() {
        return form.spec();
    }

    /** The current field-guide JSON, or 204 before the first generation. */
    @GET
    @Path("tutorial")
    @Produces(MediaType.APPLICATION_JSON)
    public Response current() {
        String json = store.get();
        if (json == null || json.isBlank()) {
            return Response.noContent().build();
        }
        return Response.ok(json).build();
    }

    /** Generate a fresh field-guide from the form. */
    @POST
    @Path("tutorial/generate")
    @Produces(MediaType.APPLICATION_JSON)
    public Response generate() {
        return runWorkflow(new TutorialRequest(form.spec(), null, null));
    }

    /** Refine the whole guide with a chat instruction. */
    @POST
    @Path("tutorial/refine")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response refine(RefineRequest request) {
        String instruction = request == null ? null : request.instruction();
        return runWorkflow(new TutorialRequest(form.spec(), instruction, store.get()));
    }

    /**
     * Refine the description of a single field. The agent receives only that
     * field's current description, updates it, and the result is merged back
     * into the full guide JSON so the other fields are preserved.
     */
    @POST
    @Path("tutorial/refine-field")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response refineField(FieldRefineRequest request) {
        if (request == null || request.fieldName() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .type(MediaType.TEXT_PLAIN)
                    .entity("A field name is required.").build();
        }
        String fullJson = store.get();
        String currentValue = extractField(fullJson, request.fieldName());
        String fieldJson = Json.createObjectBuilder()
                .add(request.fieldName(), currentValue)
                .build().toString();
        Response result = runWorkflow(new TutorialRequest(form.spec(), request.instruction(), fieldJson));
        if (result.getStatus() != Response.Status.OK.getStatusCode()) {
            return result;
        }
        String updatedValue = extractField(store.get(), request.fieldName());
        store.put(mergeField(fullJson, request.fieldName(), updatedValue));
        return Response.ok(store.get()).build();
    }

    private Response runWorkflow(TutorialRequest request) {
        try {
            trigger.fire(request);
        } catch (ObserverException wrapper) {
            Throwable cause = wrapper.getCause();
            if (cause instanceof ConstraintViolationException) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .type(MediaType.TEXT_PLAIN)
                        .entity("The request is missing required data.").build();
            }
            if (cause instanceof LLMException) {
                LOGGER.log(Level.WARNING, "LLM call failed", cause);
                return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                        .type(MediaType.TEXT_PLAIN)
                        .entity("The LLM backend is unavailable. Check that the provider "
                                + "configured in microprofile-config.properties is running.")
                        .build();
            }
            throw wrapper;
        }
        return Response.ok(store.get()).build();
    }

    private String extractField(String json, String fieldName) {
        if (json == null || json.isBlank()) return "";
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            return reader.readObject().getString(fieldName, "");
        } catch (Exception e) {
            LOGGER.warning("Could not extract field '" + fieldName + "' from guide JSON: " + e.getMessage());
            return "";
        }
    }

    private String mergeField(String fullJson, String fieldName, String newValue) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        if (fullJson != null && !fullJson.isBlank()) {
            try (JsonReader reader = Json.createReader(new StringReader(fullJson))) {
                reader.readObject().forEach(builder::add);
            } catch (Exception e) {
                LOGGER.warning("Could not parse guide JSON when merging field '" + fieldName + "': " + e.getMessage());
            }
        }
        builder.add(fieldName, newValue);
        return builder.build().toString();
    }

    public record RefineRequest(String instruction) {}

    public record FieldRefineRequest(String fieldName, String instruction) {}
}
