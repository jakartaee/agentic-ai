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
package ee.jakarta.tck.ai.agent.core.behavior.agents.eventobservation;

import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder.Phase;
import jakarta.ai.agent.Agent;
import jakarta.ai.agent.Trigger;
import jakarta.ai.agent.WorkflowScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * A {@code @WorkflowScoped} agent that observes events only through its
 * {@code @Trigger} method, which is the sole observation mechanism supported for
 * workflow-scoped agents. Dispatching the trigger requires an active workflow
 * context, so behavioral verification depends on a compatible implementation.
 */
@Agent
@WorkflowScoped
public class WorkflowScopedObserverAgent {

    @Inject ExecutionTraceRecorder trace;

    @Trigger
    public void onTriggerEvent(@Observes WorkflowScopedTriggerEvent event) {
        trace.record(Phase.TRIGGER, "onTriggerEvent", event);
    }
}