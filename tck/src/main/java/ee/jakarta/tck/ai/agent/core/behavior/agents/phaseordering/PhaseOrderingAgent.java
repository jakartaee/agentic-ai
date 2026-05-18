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
package ee.jakarta.tck.ai.agent.core.behavior.agents.phaseordering;

import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder.Phase;
import jakarta.ai.agent.Action;
import jakarta.ai.agent.Agent;
import jakarta.ai.agent.Decision;
import jakarta.ai.agent.LargeLanguageModel;
import jakarta.ai.agent.Outcome;
import jakarta.ai.agent.Result;
import jakarta.ai.agent.Trigger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@Agent
@ApplicationScoped
public class PhaseOrderingAgent {

    @Inject LargeLanguageModel llm;
    @Inject ExecutionTraceRecorder trace;

    @Trigger
    public PhaseOrderingEvent onEvent(@Observes PhaseOrderingEvent event) {
        trace.record(Phase.TRIGGER, "onEvent", event);
        return event;
    }

    @Decision
    public Result decide(PhaseOrderingEvent event) {
        trace.record(Phase.DECISION, "decide", event);
        String response = llm.query("Should we proceed?", event.payload());
        return new Result("proceed".equals(response), response);
    }

    @Action
    public String act(PhaseOrderingEvent event, Result decisionResult) {
        trace.record(Phase.ACTION, "act", event, decisionResult);
        return "action-done";
    }

    @Outcome
    public void finish(String actionResult) {
        trace.record(Phase.OUTCOME, "finish", actionResult);
    }
}
