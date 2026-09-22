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
import jakarta.ai.agent.Result;
import jakarta.ai.agent.Trigger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * Agent exercising the rule that a non-{@code null} {@code Result.details} becomes
 * part of workflow state and is injectable by its runtime type into later phases.
 *
 * <p>The {@code @Decision} returns {@code new Result(true, new DecisionDetails(...))}.
 * Because {@code success} is {@code true} the workflow proceeds, and the
 * {@code @Action} injects the {@link DecisionDetails} carried by the result — not
 * the {@link Result} wrapper itself.</p>
 */
@Agent
@ApplicationScoped
public class ResultDetailsAgent {

    @Inject ExecutionTraceRecorder trace;

    @Trigger
    public void onEvent(@Observes ResultDetailsEvent event) {
        trace.record(Phase.TRIGGER, "onEvent", event);
    }

    @Decision
    public Result decide(ResultDetailsEvent event) {
        trace.record(Phase.DECISION, "decide", event);
        return new Result(true, new DecisionDetails("details:" + event.input()));
    }

    @Action
    public void act(DecisionDetails details) {
        trace.record(Phase.ACTION, "act", details);
    }
}