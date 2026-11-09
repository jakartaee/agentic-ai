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

import ee.jakarta.tck.ai.agent.behavior.agents.phaseordering.PhaseOrderingAgent;
import ee.jakarta.tck.ai.agent.behavior.agents.phaseordering.PhaseOrderingEvent;
import ee.jakarta.tck.ai.agent.behavior.agents.phaseordering.TriggerOutput;
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
public class PhaseOrderingTests {

    @Deployment
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "phaseordering.war")
                .addClasses(PhaseOrderingAgent.class, PhaseOrderingEvent.class, TriggerOutput.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject Event<PhaseOrderingEvent> events;
    @Inject ExecutionTraceRecorder trace;
    /**
     * Full T→D→A→O pipeline executes in strict phase order.
     *
     * <p>Specification section {@code agent-lifecycle}.
     */
    @Test
    public void fullPipelineExecutesInStrictOrder() {
        trace.reset();
        events.fire(new PhaseOrderingEvent("proceed"));

        assertEquals(List.of(Phase.TRIGGER, Phase.DECISION, Phase.ACTION, Phase.OUTCOME), trace.phases());
    }
    /**
     * Trigger return value (non-void) is available for injection in Decision.
     *
     * <p>Specification section {@code agent-lifecycle}.
     */
    @Test
    public void triggerReturnValueIsInjectableInDecision() {
        trace.reset();
        events.fire(new PhaseOrderingEvent("proceed"));

        Object[] decisionArgs = trace.entries().get(1).args();
        assertInstanceOf(PhaseOrderingEvent.class, decisionArgs[0]);
        assertEquals("trigger:proceed", assertInstanceOf(TriggerOutput.class, decisionArgs[1]).fromTrigger());
    }

    /**
     * The details of a Result returned by {@code @Decision} are available for injection in {@code
     * @Action}.
     *
     * <p>Specification section {@code agent-lifecycle}.
     */
    @Test
    public void decisionDetailsAreInjectableInAction() {
        trace.reset();
        events.fire(new PhaseOrderingEvent("proceed"));

        Object[] actionArgs = trace.entries().get(2).args();
        assertInstanceOf(PhaseOrderingEvent.class, actionArgs[0]);
        assertEquals("proceed", actionArgs[1]);
    }
}
