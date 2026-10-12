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
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;
import java.util.logging.Logger;

/**
 * REST API for the customer intake form. {@link #prepare} runs the
 * {@link IntakeAgent} pipeline and hands the prepared {@link IntakeRecord}
 * back for the customer to review. {@link #confirm} takes the final record
 * (possibly edited by the customer) and stubs the hand-off that would, in a
 * real deployment, reach a CRM, ticketing system or outbound email.
 */
@Path("intake")
@RequestScoped
public class IntakeResource {

    private static final Logger LOGGER = Logger.getLogger(IntakeResource.class.getName());

    @Inject
    Event<IntakeRequest> trigger;

    @Inject
    IntakeOutput output;

    @POST
    @Path("prepare")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response prepare(@NotNull IntakeRequest request) {
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
                    .entity("We could not route this inquiry. Please add your name or email, "
                            + "or elaborate on what you are asking for.")
                    .build();
        }
        return Response.ok(record).build();
    }

    @POST
    @Path("confirm")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response confirm(@NotNull IntakeRecord record) {
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
