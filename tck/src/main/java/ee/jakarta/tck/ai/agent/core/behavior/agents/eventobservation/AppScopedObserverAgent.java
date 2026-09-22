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
package ee.jakarta.tck.ai.agent.core.behavior.agents.eventobservation;

import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder.Phase;
import jakarta.ai.agent.Agent;
import jakarta.ai.agent.Trigger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * An {@code @ApplicationScoped} agent that observes events through both a
 * {@code @Trigger} method and a general {@code @Observes} observer method, which
 * is permitted for application-scoped agents.
 *
 * <p>The general observer is standard CDI and is invoked even without a compatible
 * implementation, so it can be verified against the plain-CDI baseline.</p>
 */
@Agent
@ApplicationScoped
public class AppScopedObserverAgent {

    @Inject ExecutionTraceRecorder trace;

    @Trigger
    public void onTriggerEvent(@Observes EventObservationTriggerEvent event) {
        trace.record(Phase.TRIGGER, "onTriggerEvent", event);
    }

    /**
     * General CDI observer method (no {@code @Trigger}). Recorded under
     * {@link Phase#TRIGGER} because it is an event-observation entry point;
     * assertions distinguish it from the trigger by method name.
     *
     * @param event the observed event
     */
    public void onGeneralEvent(@Observes GeneralObservationEvent event) {
        trace.record(Phase.TRIGGER, "onGeneralEvent", event);
    }
}