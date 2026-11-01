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
package ee.jakarta.tck.ai.agent.core.behavior.agents.llm;

import ee.jakarta.tck.ai.agent.framework.llm.TckModel;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder.Phase;
import jakarta.ai.agent.Action;
import jakarta.ai.agent.Agent;
import jakarta.ai.agent.LLMException;
import jakarta.ai.agent.LargeLanguageModel;
import jakarta.ai.agent.OnException;
import jakarta.ai.agent.Outcome;
import jakarta.ai.agent.Trigger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@Agent
@ApplicationScoped
public class LlmFailureHandlingAgent {

    @Inject LargeLanguageModel llm;
    @Inject ExecutionTraceRecorder trace;

    @Trigger
    public void onEvent(@Observes LlmFailureEvent event) {
        trace.record(Phase.TRIGGER, "onEvent", event);
    }

    @Action
    public void act(LlmFailureEvent event) {
        trace.record(Phase.ACTION, "act", event);
        llm.query(TckModel.FAIL);
    }

    @OnException
    public void onLlmFailure(LLMException ex) {
        trace.record(Phase.HANDLE_EXCEPTION, "onLlmFailure", ex);
    }

    @Outcome
    public void finish() {
        trace.record(Phase.OUTCOME, "finish");
    }
}
