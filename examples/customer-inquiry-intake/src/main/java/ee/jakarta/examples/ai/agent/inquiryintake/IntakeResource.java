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

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.ObserverException;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;
import java.util.logging.Logger;

/**
 * REST API for the customer intake form. {@link #submit} runs the whole
 * {@link IntakeAgent} pipeline (synchronously, via {@code Event.fire(...)})
 * and hands the prepared {@link IntakeRecord} back for the customer to
 * review. {@link #confirm} takes the final record (possibly edited by the
 * customer) and stubs the hand-off that would, in a real deployment, reach a
 * CRM, ticketing system or outbound email.
 */
@Path("")
@RequestScoped
public class IntakeResource {

    private static final Logger LOGGER = Logger.getLogger(IntakeResource.class.getName());

    @Inject
    Event<IntakeRequest> trigger;

    @Inject
    IntakeOutput output;

    /** Run the pipeline on the raw submission; return the prepared record for review. */
    @POST
    @Path("intake")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response submit(IntakeRequest request) {
        if (request == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .type(MediaType.TEXT_PLAIN)
                    .entity("A submission is required.").build();
        }
        try {
            trigger.fire(request);
        } catch (ObserverException wrapper) {
            // CDI wraps anything thrown by a synchronous observer. The Bean Validation
            // interceptor on @Trigger(@Valid IntakeRequest) throws a ConstraintViolationException
            // when the record is invalid; map that to a 400. LLMException from an @Action is
            // caught inside the agent by @OnException and never propagates out.
            Throwable cause = wrapper.getCause();
            if (cause instanceof ConstraintViolationException cve) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .type(MediaType.TEXT_PLAIN)
                        .entity(formatViolations(cve)).build();
            }
            throw wrapper;
        }

        IntakeRecord record = output.record();
        if (record == null) {
            // A mid-pipeline @Decision returned false and the workflow stopped before
            // @Action assemble could run.
            return Response.status(422)
                    .type(MediaType.TEXT_PLAIN)
                    .entity("We could not route this inquiry. Please add your name, email, "
                            + "or company, or elaborate on what you are asking for.")
                    .build();
        }
        return Response.ok(record).build();
    }

    /** Hand the (possibly edited) record off. In this sample, we just log it. */
    @POST
    @Path("intake/confirm")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response confirm(IntakeRecord record) {
        if (record == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .type(MediaType.TEXT_PLAIN)
                    .entity("A record is required.").build();
        }
        LOGGER.info("[CONFIRM] inquiry sent: " + record);
        return Response.ok(Map.of("status", "sent")).build();
    }

    private static String formatViolations(ConstraintViolationException e) {
        StringBuilder sb = new StringBuilder("The submission has validation errors:");
        e.getConstraintViolations().forEach(v ->
                sb.append("\n  - ").append(v.getPropertyPath()).append(": ").append(v.getMessage()));
        return sb.toString();
    }
}
