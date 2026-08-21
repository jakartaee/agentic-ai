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

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.io.StringReader;
import java.util.logging.Logger;

/**
 * REST API for the tutorial UI. Firing the {@link TutorialRequest} event is
 * synchronous, so the agent workflow (including the LLM call) completes before
 * the method returns and the freshly produced HTML can be read from the
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

    /** The current tutorial HTML (empty until first generated). */
    @GET
    @Path("tutorial")
    @Produces(MediaType.TEXT_HTML)
    public String current() {
        return store.get();
    }

    /** Generate a fresh tutorial from the form. */
    @POST
    @Path("tutorial/generate")
    @Produces(MediaType.TEXT_HTML)
    public String generate() {
        trigger.fire(new TutorialRequest(form.spec(), null, null));
        return store.get();
    }

    /** Refine the whole guide with a chat instruction. */
    @POST
    @Path("tutorial/refine")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_HTML)
    public String refine(RefineRequest request) {
        String instruction = request == null ? null : request.instruction();
        trigger.fire(new TutorialRequest(form.spec(), instruction, store.get()));
        return store.get();
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
    public String refineField(FieldRefineRequest request) {
        if (request == null || request.fieldName() == null || request.instruction() == null) {
            return store.get();
        }
        String fullJson = store.get();
        String currentValue = extractField(fullJson, request.fieldName());
        String fieldJson = Json.createObjectBuilder()
                .add(request.fieldName(), currentValue)
                .build().toString();
        trigger.fire(new TutorialRequest(form.spec(), request.instruction(), fieldJson));
        String updatedValue = extractField(store.get(), request.fieldName());
        store.put(mergeField(fullJson, request.fieldName(), updatedValue));
        return store.get();
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
