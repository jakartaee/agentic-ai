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

import ee.jakarta.tck.ai.agent.core.behavior.agents.termination.BooleanTerminationAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.termination.BooleanTerminationEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.termination.ObjectTerminationAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.termination.ObjectTerminationDecision;
import ee.jakarta.tck.ai.agent.core.behavior.agents.termination.ObjectTerminationEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.termination.ResultTerminationAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.termination.ResultTerminationEvent;
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
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(ArquillianExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TerminationTests {

    @Deployment
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "termination.war")
                .addClasses(BooleanTerminationAgent.class, BooleanTerminationEvent.class,
                            ResultTerminationAgent.class,  ResultTerminationEvent.class,
                            ObjectTerminationAgent.class,  ObjectTerminationEvent.class,
                            ObjectTerminationDecision.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject Event<BooleanTerminationEvent> booleanEvents;
    @Inject Event<ResultTerminationEvent>  resultEvents;
    @Inject Event<ObjectTerminationEvent>  objectEvents;
    @Inject ExecutionTraceRecorder trace;
    /**
     * Boolean false from {@code @Decision} halts all downstream phases.
     *
     * <p>Specification section {@code agent-lifecycle}.
     */
    @Test
    public void booleanFalseTerminatesWorkflow() {
        trace.reset();
        booleanEvents.fire(new BooleanTerminationEvent("x"));
        assertEquals(List.of(Phase.TRIGGER, Phase.DECISION), trace.phases());
    }
    /**
     * Result(success=false) from {@code @Decision} halts all downstream phases.
     *
     * <p>Specification section {@code agent-lifecycle}.
     */
    @Test
    public void resultFalseTerminatesWorkflow() {
        trace.reset();
        resultEvents.fire(new ResultTerminationEvent("x"));
        assertEquals(List.of(Phase.TRIGGER, Phase.DECISION), trace.phases());
    }
    /**
     * Result(success=true) with null details from {@code @Decision} proceeds through all
     * downstream phases.
     *
     * <p>Specification section {@code agent-lifecycle}.
     */
    @Test
    public void resultTrueWithNullDetailsProceeds() {
        trace.reset();
        resultEvents.fire(new ResultTerminationEvent("proceed"));
        assertEquals(List.of(Phase.TRIGGER, Phase.DECISION, Phase.ACTION, Phase.OUTCOME), trace.phases());
    }
    /**
     * Object null from {@code @Decision} halts all downstream phases.
     *
     * <p>Specification section {@code agent-lifecycle}.
     */
    @Test
    public void objectNullTerminatesWorkflow() {
        trace.reset();
        objectEvents.fire(new ObjectTerminationEvent("x"));
        assertEquals(List.of(Phase.TRIGGER, Phase.DECISION), trace.phases());
    }
}
