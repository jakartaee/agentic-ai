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

import ee.jakarta.tck.ai.agent.core.behavior.agents.errorhandling.HierarchyEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.errorhandling.NoHandlerEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.errorhandling.PhaseFailureAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.errorhandling.PhaseFailureEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.errorhandling.PropagationEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.errorhandling.RecoveryEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.errorhandling.RecursiveGuardEvent;
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
 * The parts of {@link ee.jakarta.tck.ai.agent.core.behavior.OnExceptionTests} that plain CDI satisfies: the
 * archive deploys and the {@code @Trigger} fires. Run only in the
 * specification repository on Weld Embedded; not part of the TCK.
 */
@ExtendWith(ArquillianExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class OnExceptionTests {

    @Deployment
    public static Archive<?> createDeployment() {
        return ee.jakarta.tck.ai.agent.core.behavior.OnExceptionTests.createDeployment();
    }

    @Inject Event<RecoveryEvent>          recoveryEvents;
    @Inject Event<PropagationEvent>       propagationEvents;
    @Inject Event<HierarchyEvent>         hierarchyEvents;
    @Inject Event<RecursiveGuardEvent>    recursiveGuardEvents;
    @Inject Event<PhaseFailureEvent>      phaseFailureEvents;
    @Inject Event<NoHandlerEvent>         noHandlerEvents;
    @Inject ExecutionTraceRecorder        trace;
    @Inject PhaseFailureAgent             phaseFailureAgent;

    @Test
    public void recoveryAgentTriggerObserved() {
        trace.reset();
        recoveryEvents.fire(new RecoveryEvent("x"));
        assertEquals(List.of(Phase.TRIGGER), trace.phases());
    }

    @Test
    public void propagationAgentTriggerObserved() {
        trace.reset();
        propagationEvents.fire(new PropagationEvent("x"));
        assertEquals(List.of(Phase.TRIGGER), trace.phases());
    }

    @Test
    public void hierarchyAgentTriggerObserved() {
        trace.reset();
        hierarchyEvents.fire(new HierarchyEvent("x"));
        assertEquals(List.of(Phase.TRIGGER), trace.phases());
    }

    @Test
    public void recursiveGuardAgentTriggerObserved() {
        trace.reset();
        recursiveGuardEvents.fire(new RecursiveGuardEvent("x"));
        assertEquals(List.of(Phase.TRIGGER), trace.phases());
    }

    @Test
    public void phaseFailureAgentTriggerObserved() {
        trace.reset();
        phaseFailureAgent.setFailAt(PhaseFailureAgent.FailingPhase.NONE);
        phaseFailureEvents.fire(new PhaseFailureEvent("x"));
        assertEquals(List.of(Phase.TRIGGER), trace.phases());
    }

    @Test
    public void noHandlerAgentTriggerObserved() {
        trace.reset();
        noHandlerEvents.fire(new NoHandlerEvent("x"));
        assertEquals(List.of(Phase.TRIGGER), trace.phases());
    }
}
