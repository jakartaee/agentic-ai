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
package ee.jakarta.tck.ai.agent.behavior.agents.validation;

import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder.Phase;
import jakarta.ai.agent.Action;
import jakarta.ai.agent.Agent;
import jakarta.ai.agent.Decision;
import jakarta.ai.agent.OnException;
import jakarta.ai.agent.Trigger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;

@Agent
@ApplicationScoped
public class DecisionValidatedAgent {

    @Inject ExecutionTraceRecorder trace;

    @Trigger
    public InputData onEvent(@Observes DecisionValidationEvent event) {
        trace.record(Phase.TRIGGER, "onEvent", event);
        return new InputData(event.payload());
    }

    @Decision(order = 1)
    public boolean decide(@Valid InputData data) {
        trace.record(Phase.DECISION, "decide", data);
        return true;
    }

    @Action(order = 2)
    public void act(InputData data) {
        trace.record(Phase.ACTION, "act", data);
    }

    @OnException
    public void onInvalid(ConstraintViolationException ex) {
        trace.record(Phase.ON_EXCEPTION, "onInvalid", ex);
    }
}
