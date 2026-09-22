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
package ee.jakarta.tck.ai.agent.core.behavior.agents.inheritance;

import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder.Phase;
import jakarta.ai.agent.Agent;
import jakarta.ai.agent.Trigger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

/**
 * Agent that inherits the {@code @Action} from {@link BasePhaseAgent} without
 * overriding it. The inherited phase method participates in the workflow as if it
 * were declared on this subclass.
 */
@Agent
@ApplicationScoped
public class InheritingAgent extends BasePhaseAgent {

    @Trigger
    public void onEvent(@Observes InheritEvent event) {
        trace.record(Phase.TRIGGER, "onEvent", event);
    }
}