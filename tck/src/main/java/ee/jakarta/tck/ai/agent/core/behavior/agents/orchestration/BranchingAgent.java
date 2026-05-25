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
package ee.jakarta.tck.ai.agent.core.behavior.agents.orchestration;

import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder.Phase;
import jakarta.ai.agent.Action;
import jakarta.ai.agent.Agent;
import jakarta.ai.agent.Decision;
import jakarta.ai.agent.Outcome;
import jakarta.ai.agent.Trigger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@Agent
@ApplicationScoped
public class BranchingAgent {

    public enum TerminationPoint { FIRST_DECISION, SECOND_DECISION }

    @Inject ExecutionTraceRecorder trace;
    private volatile TerminationPoint terminateAt = TerminationPoint.FIRST_DECISION;

    public void setTerminateAt(TerminationPoint terminateAt) {
        this.terminateAt = terminateAt;
    }

    @Trigger
    public void onEvent(@Observes BranchingEvent event) {
        trace.record(Phase.TRIGGER, "onEvent", event);
    }

    @Decision
    public boolean firstDecide(BranchingEvent event) {
        trace.record(Phase.DECISION, "firstDecide", event);
        return terminateAt != TerminationPoint.FIRST_DECISION;
    }

    @Action
    public void firstAction(BranchingEvent event) {
        trace.record(Phase.ACTION, "firstAction", event);
    }

    @Decision
    public boolean secondDecide(BranchingEvent event) {
        trace.record(Phase.DECISION, "secondDecide", event);
        return terminateAt != TerminationPoint.SECOND_DECISION;
    }

    @Action
    public void secondAction(BranchingEvent event) {
        trace.record(Phase.ACTION, "secondAction", event);
    }

    @Outcome
    public void finish() {
        trace.record(Phase.OUTCOME, "finish");
    }
}