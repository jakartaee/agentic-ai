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
package ee.jakarta.tck.ai.agent.core.behavior.agents.errorhandling;

import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder.Phase;
import jakarta.ai.agent.Action;
import jakarta.ai.agent.Agent;
import jakarta.ai.agent.Decision;
import jakarta.ai.agent.HandleException;
import jakarta.ai.agent.Outcome;
import jakarta.ai.agent.Trigger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@Agent
@ApplicationScoped
public class PhaseFailureAgent {

    public enum FailingPhase { NONE, TRIGGER, DECISION, ACTION, OUTCOME }

    @Inject ExecutionTraceRecorder trace;
    public volatile FailingPhase failAt = FailingPhase.NONE;

    @Trigger
    public void onEvent(@Observes PhaseFailureEvent event) {
        trace.record(Phase.TRIGGER, "onEvent", event);
        if (failAt == FailingPhase.TRIGGER)
            throw new AgentDomainException("trigger failure");
    }

    @Decision
    public boolean decide(PhaseFailureEvent event) {
        trace.record(Phase.DECISION, "decide", event);
        if (failAt == FailingPhase.DECISION)
            throw new AgentDomainException("decision failure");
        return true;
    }

    @Action
    public void act(PhaseFailureEvent event) {
        trace.record(Phase.ACTION, "act", event);
        if (failAt == FailingPhase.ACTION)
            throw new AgentDomainException("action failure");
    }

    @Outcome
    public void finish() {
        trace.record(Phase.OUTCOME, "finish");
        if (failAt == FailingPhase.OUTCOME)
            throw new AgentDomainException("outcome failure");
    }

    @HandleException
    public void onError(AgentDomainException ex) {
        trace.record(Phase.HANDLE_EXCEPTION, "onError", ex);
    }
}
