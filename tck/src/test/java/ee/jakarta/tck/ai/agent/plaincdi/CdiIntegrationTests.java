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

import ee.jakarta.tck.ai.agent.core.behavior.agents.cdi.InterceptedEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.cdi.ResolutionOrderEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.cdi.SingletonCdiEvent;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder.Phase;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder;
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
 * The parts of {@link ee.jakarta.tck.ai.agent.core.behavior.CdiIntegrationTests} that plain CDI satisfies: the
 * archive deploys and the {@code @Trigger} fires. Run only in the
 * specification repository on Weld Embedded; not part of the TCK.
 */
@ExtendWith(ArquillianExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CdiIntegrationTests {

    @Deployment
    public static Archive<?> createDeployment() {
        return ee.jakarta.tck.ai.agent.core.behavior.CdiIntegrationTests.createDeployment();
    }

    @Inject Event<SingletonCdiEvent>        singletonEvents;
    @Inject Event<InterceptedEvent>         interceptedEvents;
    @Inject Event<ResolutionOrderEvent>     resolutionOrderEvents;
    @Inject ExecutionTraceRecorder          trace;

    @Test
    public void singletonAgentTriggerObserved() {
        trace.reset();
        singletonEvents.fire(new SingletonCdiEvent("test"));
        assertEquals(List.of(Phase.TRIGGER), trace.phases());
    }

    @Test
    public void interceptedAgentTriggerObserved() {
        trace.reset();
        interceptedEvents.fire(new InterceptedEvent("test"));
        assertEquals(List.of(Phase.TRIGGER), trace.phases());
    }

    @Test
    public void resolutionOrderAgentTriggerObserved() {
        trace.reset();
        resolutionOrderEvents.fire(new ResolutionOrderEvent("test"));
        assertEquals(List.of(Phase.TRIGGER), trace.phases());
    }
}
