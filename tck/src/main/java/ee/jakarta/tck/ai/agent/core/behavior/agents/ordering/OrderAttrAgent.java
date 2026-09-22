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
import jakarta.ai.agent.Decision;
import jakarta.ai.agent.Trigger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * Agent that orders its {@code @Action}/{@code @Decision} methods with the
 * {@code order} attribute. The methods are declared in the reverse of their
 * intended execution order, so a trace that follows the {@code order} sort key
 * (rather than source declaration order) proves the attribute is honored at
 * runtime.
 *
 * <p>Per the spec consistency requirement, every {@code @Action}/{@code @Decision}
 * method here declares an explicit {@code order}.</p>
 */
@Agent
@ApplicationScoped
public class OrderAttrAgent {

    @Inject
    ExecutionTraceRecorder trace;

    @Trigger
    public void onEvent(@Observes OrderAttrEvent event) {
        trace.record(Phase.TRIGGER, "onEvent", event);
    }

    @Action(order = 3)
    public void third() {
        trace.record(Phase.ACTION, "third");
    }

    @Decision(order = 2)
    public boolean second() {
        trace.record(Phase.DECISION, "second");
        return true; // continue
    }

    @Action(order = 1)
    public void first() {
        trace.record(Phase.ACTION, "first");
    }
}