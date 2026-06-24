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
import jakarta.ai.agent.WorkflowScoped;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.util.UUID;

@Agent(name = "lifecycleSpy")
@WorkflowScoped
public class LifecycleSpyAgent {

    // Static so the failure-path test can configure it without injecting this
    // @WorkflowScoped bean (no active context outside the compatible implementation). Default false keeps
    // the success-path tests unchanged.
    private static volatile boolean failInAction = false;
    public static void setFailInAction(boolean value) { failInAction = value; }

    private String instanceId;

    @Inject ExecutionTraceRecorder    trace;
    @Inject LifecycleCallbackRecorder lifecycleRecorder;

    @PostConstruct
    void init() {
        instanceId = UUID.randomUUID().toString();
        lifecycleRecorder.recordPostConstruct(instanceId);
    }

    @PreDestroy
    void destroy() {
        lifecycleRecorder.recordPreDestroy();
    }

    @Trigger
    public void onEvent(@Observes LifecycleSpyEvent event) {
        trace.record(Phase.TRIGGER, "onEvent", event, instanceId);
    }

    @Action
    public void process(LifecycleSpyEvent event) {
        trace.record(Phase.ACTION, "process", event, instanceId);
        if (failInAction) {
            throw new IllegalStateException("Simulated @Action failure for @PreDestroy-on-failure test");
        }
    }

    @Outcome
    public void finish(LifecycleSpyEvent event) {
        trace.record(Phase.OUTCOME, "finish", event, instanceId);
    }
}
