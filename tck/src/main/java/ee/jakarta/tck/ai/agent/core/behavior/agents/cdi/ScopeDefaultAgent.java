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
import jakarta.ai.agent.Outcome;
import jakarta.ai.agent.Trigger;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.util.UUID;

// Intentionally NO scope annotation — the compatible implementation must default to @WorkflowScoped.
// Under annotated discovery mode this class is NOT a CDI bean.
@Agent
public class ScopeDefaultAgent {

    private String instanceId;

    @Inject ExecutionTraceRecorder    trace;
    @Inject LifecycleCallbackRecorder lifecycleRecorder;

    @PostConstruct
    void init() {
        instanceId = UUID.randomUUID().toString();
        lifecycleRecorder.recordPostConstruct(instanceId);
    }

    @Trigger
    public void onEvent(@Observes ScopeDefaultEvent event) {
        trace.record(Phase.TRIGGER, "onEvent", event, instanceId);
    }

    @Action
    public void process(ScopeDefaultEvent event) {
        trace.record(Phase.ACTION, "process", event, instanceId);
    }

    @Outcome
    public void finish(ScopeDefaultEvent event) {
        trace.record(Phase.OUTCOME, "finish", event, instanceId);
    }
}
