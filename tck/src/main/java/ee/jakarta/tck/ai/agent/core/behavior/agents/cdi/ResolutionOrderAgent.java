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
package ee.jakarta.tck.ai.agent.core.behavior.agents.cdi;

import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder.Phase;
import jakarta.ai.agent.Action;
import jakarta.ai.agent.Agent;
import jakarta.ai.agent.Decision;
import jakarta.ai.agent.LargeLanguageModel;
import jakarta.ai.agent.Outcome;
import jakarta.ai.agent.Trigger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@Agent
@ApplicationScoped
public class ResolutionOrderAgent {

    @Inject ExecutionTraceRecorder trace;

    @Trigger
    public void onEvent(@Observes ResolutionOrderEvent event) {
        trace.record(Phase.TRIGGER, "onEvent", event);
    }

    // Priority 1 (event) + Priority 3 (LLM)
    @Decision
    public boolean decide(ResolutionOrderEvent event, LargeLanguageModel llm) {
        trace.record(Phase.DECISION, "decide", event, llm);
        return true;
    }

    // Priority 1 (event) + Priority 3 (LLM) + Priority 4 (CDI bean)
    @Action
    public void process(ResolutionOrderEvent event, LargeLanguageModel llm, ResolutionCdiBean service) {
        trace.record(Phase.ACTION, "process", event, llm, service);
    }

    // Priority 1 (event) + Priority 4 (CDI bean)
    @Outcome
    public void finish(ResolutionOrderEvent event, ResolutionCdiBean service) {
        trace.record(Phase.OUTCOME, "finish", event, service);
    }
}
