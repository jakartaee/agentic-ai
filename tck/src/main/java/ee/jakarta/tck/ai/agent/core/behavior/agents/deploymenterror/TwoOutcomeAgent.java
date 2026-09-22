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

import jakarta.ai.agent.Agent;
import jakarta.ai.agent.Outcome;
import jakarta.ai.agent.Trigger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

/**
 * Invalid agent (case G3d, <em>@Outcome cardinality</em>): the agent declares two
 * {@code @Outcome} methods. An agent may declare zero or one {@code @Outcome}, so
 * a compatible implementation must reject this agent at deployment time.
 */
@Agent
@ApplicationScoped
public class TwoOutcomeAgent {

    @Trigger
    public void onEvent(@Observes DeploymentErrorEvent event) {
    }

    @Outcome
    public void firstOutcome() {
    }

    @Outcome
    public void secondOutcome() {
    }
}