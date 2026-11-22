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
package ee.jakarta.tck.ai.agent.agents.deploymenterror;

import jakarta.ai.agent.Agent;
import jakarta.ai.agent.Trigger;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;

/**
 * Invalid agent for the agent scope rule. The agent declares {@code @Dependent}.
 * An agent must be {@code @WorkflowScoped} or {@code @ApplicationScoped}, so a
 * compatible implementation must reject this agent at deployment time.
 */
@Agent
@Dependent
public class DependentScopedAgent {

    @Trigger
    public void onEvent(@Observes DeploymentErrorEvent event) {
    }
}
