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

import ee.jakarta.tck.ai.agent.core.behavior.agents.datapropagation.ActionOutput;
import ee.jakarta.tck.ai.agent.core.behavior.agents.datapropagation.DataPropagationAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.datapropagation.DataPropagationEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.datapropagation.DecisionOutput;
import ee.jakarta.tck.ai.agent.core.behavior.agents.datapropagation.TriggerOutput;
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
public class DataPropagationTests {

    @Deployment
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "datapropagation.war")
                .addClasses(DataPropagationAgent.class, DataPropagationEvent.class,
                            TriggerOutput.class, DecisionOutput.class, ActionOutput.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject Event<DataPropagationEvent> events;
    @Inject ExecutionTraceRecorder trace;
    /**
     * TriggerOutput returned by {@code @Trigger} is injectable as a parameter in {@code
     * @Decision}.
     *
     * <p>Specification section {@code parameter-resolution}.
     */
    @Test
    public void triggerOutputIsInjectableInDecision() {
        trace.reset();
        events.fire(new DataPropagationEvent("input"));
        assertInstanceOf(TriggerOutput.class, trace.entries().get(1).args()[1]);
    }
    /**
     * DecisionOutput returned by {@code @Decision} is injectable as a parameter in {@code
     * @Action}.
     *
     * <p>Specification section {@code parameter-resolution}.
     */
    @Test
    public void decisionOutputIsInjectableInAction() {
        trace.reset();
        events.fire(new DataPropagationEvent("input"));
        DecisionOutput decision = assertInstanceOf(DecisionOutput.class, trace.entries().get(2).args()[1]);
        assertEquals("decision:trigger:input", decision.fromDecision());
    }
    /**
     * {@code @Outcome} can inject objects from all preceding phases simultaneously.
     *
     * <p>Specification section {@code parameter-resolution}.
     */
    @Test
    public void allPhaseOutputsAreInjectableInOutcome() {
        trace.reset();
        events.fire(new DataPropagationEvent("input"));

        assertEquals(List.of(Phase.TRIGGER, Phase.DECISION, Phase.ACTION, Phase.OUTCOME), trace.phases());

        Object[] outcomeArgs = trace.entries().get(3).args();
        assertInstanceOf(DataPropagationEvent.class, outcomeArgs[0]);
        assertInstanceOf(TriggerOutput.class, outcomeArgs[1]);
        assertInstanceOf(DecisionOutput.class, outcomeArgs[2]);
        assertInstanceOf(ActionOutput.class, outcomeArgs[3]);
    }
}
