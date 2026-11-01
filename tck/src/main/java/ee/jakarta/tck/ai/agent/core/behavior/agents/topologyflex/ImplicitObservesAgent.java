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
package ee.jakarta.tck.ai.agent.core.behavior.agents.topologyflex;

import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder.Phase;
import jakarta.ai.agent.Action;
import jakarta.ai.agent.Agent;
import jakarta.ai.agent.Outcome;
import jakarta.ai.agent.Trigger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Declares its trigger without {@code @Observes}, the optional form the
 * specification's own examples use.
 */
@Agent
@ApplicationScoped
public class ImplicitObservesAgent {

    @Inject ExecutionTraceRecorder trace;

    @Trigger
    public void onEvent(ImplicitObservesEvent event) {
        trace.record(Phase.TRIGGER, "onEvent", event);
    }

    @Action
    public void act(ImplicitObservesEvent event) {
        trace.record(Phase.ACTION, "act", event);
    }

    @Outcome
    public void finish(ImplicitObservesEvent event) {
        trace.record(Phase.OUTCOME, "finish", event);
    }
}
