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

import ee.jakarta.tck.ai.agent.core.behavior.agents.orchestration.AnchoredEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.orchestration.BranchingAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.orchestration.BranchingEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.orchestration.IntermixedEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.orchestration.LinearEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.orchestration.OutcomeOnlyEvent;
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
 * The parts of {@link ee.jakarta.tck.ai.agent.core.behavior.OrchestrationTests} that plain CDI satisfies: the
 * archive deploys and the {@code @Trigger} fires. Run only in the
 * specification repository on Weld Embedded; not part of the TCK.
 */
@ExtendWith(ArquillianExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class OrchestrationTests {

    @Deployment
    public static Archive<?> createDeployment() {
        return ee.jakarta.tck.ai.agent.core.behavior.OrchestrationTests.createDeployment();
    }

    @Inject Event<LinearEvent>      linearEvents;
    @Inject Event<IntermixedEvent>  intermixedEvents;
    @Inject Event<BranchingEvent>   branchingEvents;
    @Inject Event<OutcomeOnlyEvent> outcomeOnlyEvents;
    @Inject Event<AnchoredEvent>    anchoredEvents;
    @Inject ExecutionTraceRecorder  trace;
    @Inject BranchingAgent          branchingAgent;

    @Test
    public void linearAgentTriggerObserved() {
        trace.reset();
        linearEvents.fire(new LinearEvent("test"));
        assertEquals(List.of(Phase.TRIGGER), trace.phases());
    }

    @Test
    public void intermixedAgentTriggerObserved() {
        trace.reset();
        intermixedEvents.fire(new IntermixedEvent("test"));
        assertEquals(List.of(Phase.TRIGGER), trace.phases());
    }

    @Test
    public void branchingAgentTriggerObserved() {
        trace.reset();
        branchingAgent.setTerminateAt(BranchingAgent.TerminationPoint.FIRST_DECISION);
        branchingEvents.fire(new BranchingEvent("test"));
        assertEquals(List.of(Phase.TRIGGER), trace.phases());
    }

    @Test
    public void outcomeOnlyAgentTriggerObserved() {
        trace.reset();
        outcomeOnlyEvents.fire(new OutcomeOnlyEvent("test"));
        assertEquals(List.of(Phase.TRIGGER), trace.phases());
    }

    @Test
    public void anchoredAgentTriggerObserved() {
        trace.reset();
        anchoredEvents.fire(new AnchoredEvent("test"));
        assertEquals(List.of(Phase.TRIGGER), trace.phases());
    }
}
