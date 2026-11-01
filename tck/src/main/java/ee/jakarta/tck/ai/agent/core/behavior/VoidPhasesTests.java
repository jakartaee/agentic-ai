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
package ee.jakarta.tck.ai.agent.core.behavior;

import ee.jakarta.tck.ai.agent.core.behavior.agents.voidphases.VoidPhasesAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.voidphases.VoidPhasesEvent;
import ee.jakarta.tck.ai.agent.framework.junit.anno.Assertion;
import ee.jakarta.tck.ai.agent.framework.junit.anno.Deployed;
import ee.jakarta.tck.ai.agent.framework.junit.anno.RequiresImplementation;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder.Phase;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@Deployed
public class VoidPhasesTests {

    @Deployment
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "voidphases.war")
                .addClasses(VoidPhasesAgent.class, VoidPhasesEvent.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject Event<VoidPhasesEvent> events;
    @Inject ExecutionTraceRecorder trace;


    @RequiresImplementation
    @Assertion(id = "AGENTICAI-VOID-002",
               section = "agent-lifecycle",
               strategy = "void @Trigger does not pollute the injection context; @Decision receives only the original event")
    public void voidTriggerDoesNotPollutInjectionContext() {
        trace.reset();
        events.fire(new VoidPhasesEvent("test"));

        assertEquals(List.of(Phase.TRIGGER, Phase.DECISION, Phase.ACTION, Phase.OUTCOME), trace.phases());
        Object[] decisionArgs = trace.entries().get(1).args();
        assertEquals(1, decisionArgs.length);
        assertInstanceOf(VoidPhasesEvent.class, decisionArgs[0]);
    }

    @RequiresImplementation
    @Assertion(id = "AGENTICAI-VOID-003",
               section = "agent-lifecycle",
               strategy = "void @Action does not break the injection context for @Outcome")
    public void voidActionDoesNotBreakOutcome() {
        trace.reset();
        events.fire(new VoidPhasesEvent("test"));

        assertEquals(List.of(Phase.TRIGGER, Phase.DECISION, Phase.ACTION, Phase.OUTCOME), trace.phases());
        assertInstanceOf(VoidPhasesEvent.class, trace.entries().stream()
                .filter(e -> e.phase() == Phase.OUTCOME)
                .findFirst()
                .orElseThrow()
                .args()[0]);
    }
}
