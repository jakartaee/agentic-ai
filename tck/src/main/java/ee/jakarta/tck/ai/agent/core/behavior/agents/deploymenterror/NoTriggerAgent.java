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
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Invalid agent (case G3c, <em>@Trigger cardinality</em>): the agent declares no
 * {@code @Trigger} method. There must be exactly one {@code @Trigger} per agent,
 * so a compatible implementation must reject this agent at deployment time.
 */
@Agent
@ApplicationScoped
public class NoTriggerAgent {

    @Action
    public void act() {
    }
}