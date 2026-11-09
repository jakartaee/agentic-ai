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
package ee.jakarta.tck.ai.agent.agents.validation;

import ee.jakarta.tck.ai.agent.agents.onexception.AgentDomainException;
import ee.jakarta.tck.ai.agent.framework.ExecutionTraceRecorder;
import ee.jakarta.tck.ai.agent.framework.ExecutionTraceRecorder.Phase;
import jakarta.ai.agent.Action;
import jakarta.ai.agent.Agent;
import jakarta.ai.agent.OnException;
import jakarta.ai.agent.Trigger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.validation.Valid;

/**
 * Agent whose {@code @OnException} method declares a {@code @Valid} parameter.
 * The {@code @Trigger} returns a {@link HandlerInput} built from the event, the
 * {@code @Action} always fails, and the handler would receive the input. When
 * the input is invalid, validation of the handler's parameters fails and the
 * {@code ConstraintViolationException} must propagate to the container rather
 * than re-enter exception handling; the handler body must not run.
 */
@Agent
@ApplicationScoped
public class HandlerValidatedAgent {

    @Inject
    ExecutionTraceRecorder trace;

    @Trigger
    public HandlerInput onEvent(@Observes HandlerValidationEvent event) {
        trace.record(Phase.TRIGGER, "onEvent", event);
        return new HandlerInput(event.note());
    }

    @Action
    public void act(HandlerValidationEvent event) {
        trace.record(Phase.ACTION, "act", event);
        throw new AgentDomainException("action failed");
    }

    @OnException
    public void handle(AgentDomainException ex, @Valid HandlerInput input) {
        trace.record(Phase.ON_EXCEPTION, "handle", ex, input);
    }
}
