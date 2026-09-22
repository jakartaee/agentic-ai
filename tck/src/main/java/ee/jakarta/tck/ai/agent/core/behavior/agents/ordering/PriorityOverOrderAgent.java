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
package ee.jakarta.tck.ai.agent.core.behavior.agents.ordering;

import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder.Phase;
import jakarta.ai.agent.Action;
import jakarta.ai.agent.Agent;
import jakarta.ai.agent.Trigger;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * Agent whose {@code @Action} methods declare <em>conflicting</em>
 * {@code @Priority} and {@code order} values: the {@code order} attribute would
 * sort them one way while {@code @Priority} sorts them the other. Because
 * {@code @Priority} takes precedence over {@code order}, execution must follow
 * {@code @Priority}: {@code runsFirst} (priority 1) before {@code runsThird}
 * (priority 3).
 */
@Agent
@ApplicationScoped
public class PriorityOverOrderAgent {

    @Inject
    ExecutionTraceRecorder trace;

    @Trigger
    public void onEvent(@Observes PriorityOverOrderEvent event) {
        trace.record(Phase.TRIGGER, "onEvent", event);
    }

    // order says this runs first, but @Priority(3) says it runs last.
    @Action(order = 1)
    @Priority(3)
    public void runsThird() {
        trace.record(Phase.ACTION, "runsThird");
    }

    // order says this runs last, but @Priority(1) says it runs first.
    @Action(order = 3)
    @Priority(1)
    public void runsFirst() {
        trace.record(Phase.ACTION, "runsFirst");
    }
}