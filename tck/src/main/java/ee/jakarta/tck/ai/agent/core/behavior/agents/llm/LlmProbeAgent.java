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

import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder.Phase;
import jakarta.ai.agent.Agent;
import jakarta.ai.agent.LargeLanguageModel;
import jakarta.ai.agent.Trigger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * Applies a probe to the implementation's facade inside a workflow, so the
 * facade is exercised where the specification defines it, and records the
 * result or the exception for the test to assert on.
 */
@Agent
@ApplicationScoped
public class LlmProbeAgent {

    @Inject LargeLanguageModel llm;
    @Inject ExecutionTraceRecorder trace;

    @Trigger
    public void onProbe(@Observes LlmProbeEvent event) {
        Object result;
        try {
            result = event.probe().apply(llm);
        } catch (RuntimeException e) {
            result = e;
        }
        trace.record(Phase.TRIGGER, "onProbe", result);
    }
}
