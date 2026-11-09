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
package ee.jakarta.tck.ai.agent.behavior.agents.deploymenterror;

import jakarta.ai.agent.Action;
import jakarta.ai.agent.Agent;
import jakarta.ai.agent.Trigger;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

/**
 * Invalid agent (case G3b, <em>Consistency Requirement</em>, {@code @Priority}
 * variant): one {@code @Action} declares {@code @Priority} while another declares
 * neither {@code @Priority} nor {@code order}. The rule that forbids mixing
 * explicitly ordered and unordered methods applies to {@code @Priority} exactly
 * as it does to {@code order}, so a compatible implementation must reject this
 * agent.
 */
@Agent
@ApplicationScoped
public class MixedPriorityAgent {

    @Trigger
    public void onEvent(@Observes DeploymentErrorEvent event) {
    }

    @Action
    @Priority(1)
    public void prioritized() {
    }

    @Action
    public void unordered() {
    }
}
