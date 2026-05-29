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
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.util.concurrent.atomic.AtomicInteger;

@Agent
@ApplicationScoped
public class SingletonCdiAgent {

    private static final AtomicInteger INSTANCE_COUNTER = new AtomicInteger(0);
    private int instanceId;

    @Inject ExecutionTraceRecorder trace;

    @PostConstruct
    void init() {
        instanceId = INSTANCE_COUNTER.incrementAndGet();
    }

    public static int getInstanceCount() { return INSTANCE_COUNTER.get(); }
    public int getInstanceId()           { return instanceId; }

    @Trigger
    public void onEvent(@Observes SingletonCdiEvent event) {
        trace.record(Phase.TRIGGER, "onEvent", event, instanceId);
    }

    @Action
    public void process(SingletonCdiEvent event) {
        trace.record(Phase.ACTION, "process", event, instanceId);
    }

    @Outcome
    public void finish(SingletonCdiEvent event) {
        trace.record(Phase.OUTCOME, "finish", event, instanceId);
    }
}
