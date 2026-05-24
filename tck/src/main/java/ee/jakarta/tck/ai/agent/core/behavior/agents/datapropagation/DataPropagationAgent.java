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
package ee.jakarta.tck.ai.agent.core.behavior.agents.datapropagation;

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
public class DataPropagationAgent {

    @Inject ExecutionTraceRecorder trace;
    @Inject LargeLanguageModel llm;

    @Trigger
    public TriggerOutput onEvent(@Observes DataPropagationEvent event) {
        trace.record(Phase.TRIGGER, "onEvent", event);
        return new TriggerOutput("trigger:" + event.input());
    }

    @Decision
    public DecisionOutput decide(DataPropagationEvent event, TriggerOutput trigger) {
        trace.record(Phase.DECISION, "decide", event, trigger);
        String response = llm.query("decide", trigger.fromTrigger());
        return new DecisionOutput("decision:" + response);
    }

    @Action
    public ActionOutput act(TriggerOutput trigger, DecisionOutput decision) {
        trace.record(Phase.ACTION, "act", trigger, decision);
        return new ActionOutput("action:" + trigger.fromTrigger() + "+" + decision.fromDecision());
    }

    @Outcome
    public void finish(DataPropagationEvent event, TriggerOutput trigger,
                       DecisionOutput decision, ActionOutput action) {
        trace.record(Phase.OUTCOME, "finish", event, trigger, decision, action);
    }
}
