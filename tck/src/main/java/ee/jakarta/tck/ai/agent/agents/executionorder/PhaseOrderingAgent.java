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
package ee.jakarta.tck.ai.agent.agents.executionorder;

import ee.jakarta.tck.ai.agent.framework.ExecutionTraceRecorder;
import ee.jakarta.tck.ai.agent.framework.ExecutionTraceRecorder.Phase;
import jakarta.ai.agent.Action;
import jakarta.ai.agent.Agent;
import jakarta.ai.agent.Decision;
import jakarta.ai.agent.Outcome;
import jakarta.ai.agent.Trigger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * An agent with one phase of each kind, declared in execution order, whose
 * decision always proceeds. It exists to show the full trigger, decision,
 * action, outcome sequence.
 */
@Agent
@ApplicationScoped
public class PhaseOrderingAgent {

    @Inject ExecutionTraceRecorder trace;

    @Trigger
    public void onEvent(@Observes PhaseOrderingEvent event) {
        trace.record(Phase.TRIGGER, "onEvent", event);
    }

    @Decision(order = 1)
    public boolean decide(PhaseOrderingEvent event) {
        trace.record(Phase.DECISION, "decide", event);
        return true;
    }

    @Action(order = 2)
    public void act(PhaseOrderingEvent event) {
        trace.record(Phase.ACTION, "act", event);
    }

    @Outcome
    public void finish(PhaseOrderingEvent event) {
        trace.record(Phase.OUTCOME, "finish", event);
    }
}
