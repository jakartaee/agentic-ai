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
package ee.jakarta.tck.ai.agent.core.behavior.agents.concurrency;

import jakarta.ai.agent.Action;
import jakarta.ai.agent.Agent;
import jakarta.ai.agent.LargeLanguageModel;
import jakarta.ai.agent.Outcome;
import jakarta.ai.agent.Trigger;
import jakarta.ai.agent.WorkflowScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * A {@code @WorkflowScoped} agent used to verify that concurrent workflow
 * executions are isolated from one another.
 *
 * <p>The trigger stores the event's unique marker in an instance field and then
 * waits at a shared barrier so every concurrent workflow's instance is alive at
 * the same time. If the implementation correctly gives each workflow its own
 * instance, the marker read back in {@code @Outcome} always matches the marker the
 * workflow was triggered with; a shared or thread-unsafe instance would let one
 * workflow overwrite another's field. The action additionally drives the injected
 * {@link LargeLanguageModel} with the per-workflow marker so recorded calls can be
 * checked for cross-workflow corruption.</p>
 */
@Agent
@WorkflowScoped
public class ConcurrentIsolationAgent {

    @Inject LargeLanguageModel llm;
    @Inject ConcurrencyCoordinator coordinator;

    private String marker;

    @Trigger
    public void onEvent(@Observes ConcurrencyEvent event) {
        this.marker = event.marker();
        coordinator.awaitAllStarted();
    }

    @Action
    public void queryModel(ConcurrencyEvent event) {
        llm.query("turn:{}", marker);
    }

    @Outcome
    public void finish(ConcurrencyEvent event) {
        coordinator.recordObservation(event.marker(), this.marker);
    }
}