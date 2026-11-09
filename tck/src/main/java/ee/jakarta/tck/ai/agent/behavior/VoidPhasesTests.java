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
package ee.jakarta.tck.ai.agent.behavior;

import ee.jakarta.tck.ai.agent.behavior.agents.voidphases.VoidPhasesAgent;
import ee.jakarta.tck.ai.agent.behavior.agents.voidphases.VoidPhasesEvent;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder.Phase;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@ExtendWith(ArquillianExtension.class)
public class VoidPhasesTests {

    @Deployment
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "voidphases.war")
                .addClasses(VoidPhasesAgent.class, VoidPhasesEvent.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject Event<VoidPhasesEvent> events;
    @Inject ExecutionTraceRecorder trace;
    /**
     * void {@code @Trigger} does not pollute the injection context; {@code @Decision} receives
     * only the original event.
     *
     * <p>Specification section {@code agent-lifecycle}.
     */
    @Test
    public void voidTriggerDoesNotPollutInjectionContext() {
        trace.reset();
        events.fire(new VoidPhasesEvent("test"));

        assertEquals(List.of(Phase.TRIGGER, Phase.DECISION, Phase.ACTION, Phase.OUTCOME), trace.phases());
        Object[] decisionArgs = trace.entries().get(1).args();
        assertEquals(1, decisionArgs.length);
        assertInstanceOf(VoidPhasesEvent.class, decisionArgs[0]);
    }
    /**
     * void {@code @Action} does not break the injection context for {@code @Outcome}.
     *
     * <p>Specification section {@code agent-lifecycle}.
     */
    @Test
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
