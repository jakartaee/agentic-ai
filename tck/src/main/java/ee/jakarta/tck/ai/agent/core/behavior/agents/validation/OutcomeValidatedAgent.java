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
package ee.jakarta.tck.ai.agent.core.behavior.agents.validation;

import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder.Phase;
import jakarta.ai.agent.Action;
import jakarta.ai.agent.Agent;
import jakarta.ai.agent.OnException;
import jakarta.ai.agent.Outcome;
import jakarta.ai.agent.Trigger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;

/**
 * The {@code @Action} returns an {@link InputData} built from the event payload
 * (return values are not validated) and the {@code @Outcome} receives it with
 * {@code @Valid}, so a {@code null} payload fails validation at the outcome.
 */
@Agent
@ApplicationScoped
public class OutcomeValidatedAgent {

    @Inject
    ExecutionTraceRecorder trace;

    @Trigger
    public void onEvent(@Observes OutcomeValidationEvent event) {
        trace.record(Phase.TRIGGER, "onEvent", event);
    }

    @Action
    public InputData act(OutcomeValidationEvent event) {
        trace.record(Phase.ACTION, "act", event);
        return new InputData(event.payload());
    }

    @Outcome
    public void finish(@Valid InputData data) {
        trace.record(Phase.OUTCOME, "finish", data);
    }

    @OnException
    public void onInvalid(ConstraintViolationException ex) {
        trace.record(Phase.HANDLE_EXCEPTION, "onInvalid", ex);
    }
}
