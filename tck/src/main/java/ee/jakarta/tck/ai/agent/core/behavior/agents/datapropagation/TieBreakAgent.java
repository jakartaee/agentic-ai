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
import jakarta.ai.agent.Outcome;
import jakarta.ai.agent.Trigger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * Agent exercising the parameter-resolution tie-break rule: when several earlier
 * phases have returned objects of the same type, a later phase must be injected
 * with the <em>most recently returned</em> instance.
 *
 * <p>Both {@code @Decision} and {@code @Action} return a {@link SameTypeOutput},
 * tagged {@code "decision"} and {@code "action"} respectively. The {@code @Outcome}
 * injects a single {@code SameTypeOutput}; a compatible implementation must supply
 * the {@code "action"} instance because it was returned last.</p>
 */
@Agent
@ApplicationScoped
public class TieBreakAgent {

    @Inject ExecutionTraceRecorder trace;

    @Trigger
    public void onEvent(@Observes TieBreakEvent event) {
        trace.record(Phase.TRIGGER, "onEvent", event);
    }

    @Decision
    public SameTypeOutput decide(TieBreakEvent event) {
        trace.record(Phase.DECISION, "decide", event);
        return new SameTypeOutput("decision");
    }

    @Action
    public SameTypeOutput act(TieBreakEvent event) {
        trace.record(Phase.ACTION, "act", event);
        return new SameTypeOutput("action");
    }

    @Outcome
    public void finish(SameTypeOutput latest) {
        trace.record(Phase.OUTCOME, "finish", latest);
    }
}