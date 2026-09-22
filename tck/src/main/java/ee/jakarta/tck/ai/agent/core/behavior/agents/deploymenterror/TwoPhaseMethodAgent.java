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
package ee.jakarta.tck.ai.agent.core.behavior.agents.deploymenterror;

import jakarta.ai.agent.Action;
import jakarta.ai.agent.Agent;
import jakarta.ai.agent.Decision;
import jakarta.ai.agent.Trigger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

/**
 * Invalid agent: a single method is annotated with two phase annotations
 * ({@code @Action} and {@code @Decision}). The spec (<em>One Phase per Method</em>)
 * declares this a deployment error, so a compatible implementation must reject
 * this agent at deployment time.
 */
@Agent
@ApplicationScoped
public class TwoPhaseMethodAgent {

    @Trigger
    public void onEvent(@Observes TwoPhaseMethodEvent event) {
    }

    @Action
    @Decision
    public boolean doStuff() {
        return true;
    }
}