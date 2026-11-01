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
package ee.jakarta.tck.ai.agent.plaincdi;

import ee.jakarta.tck.ai.agent.core.behavior.agents.eventobservation.EventObservationTriggerEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.eventobservation.GeneralObservationEvent;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder.Phase;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder.TraceEntry;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The parts of {@link ee.jakarta.tck.ai.agent.core.behavior.EventObservationTests} that plain CDI satisfies: the
 * archive deploys and the {@code @Trigger} fires. Run only in the
 * specification repository on Weld Embedded; not part of the TCK.
 */
@ExtendWith(ArquillianExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EventObservationTests {

    @Deployment
    public static Archive<?> createDeployment() {
        return ee.jakarta.tck.ai.agent.core.behavior.EventObservationTests.createDeployment();
    }

    @Inject Event<EventObservationTriggerEvent> triggerEvents;
    @Inject Event<GeneralObservationEvent> generalEvents;
    @Inject ExecutionTraceRecorder trace;

    private List<String> recordedMethodNames() {
        return trace.entries().stream().map(TraceEntry::methodName).toList();
    }

    @Test
    public void applicationScopedAgentObservesViaGeneralObserver() {
        trace.reset();
        generalEvents.fire(new GeneralObservationEvent("input"));
        assertEquals(List.of("onGeneralEvent"), recordedMethodNames());
    }

    @Test
    public void applicationScopedAgentObservesViaTrigger() {
        trace.reset();
        triggerEvents.fire(new EventObservationTriggerEvent("input"));
        assertEquals(List.of(Phase.TRIGGER), trace.phases());
        assertEquals(List.of("onTriggerEvent"), recordedMethodNames());
    }
}
