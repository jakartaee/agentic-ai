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
package ee.jakarta.tck.ai.agent;

import ee.jakarta.tck.ai.agent.agents.termination.BooleanTerminationAgent;
import ee.jakarta.tck.ai.agent.agents.termination.BooleanTerminationEvent;
import ee.jakarta.tck.ai.agent.agents.termination.BranchingAgent;
import ee.jakarta.tck.ai.agent.agents.termination.BranchingEvent;
import ee.jakarta.tck.ai.agent.agents.termination.ObjectTerminationAgent;
import ee.jakarta.tck.ai.agent.agents.termination.ObjectTerminationDecision;
import ee.jakarta.tck.ai.agent.agents.termination.ObjectTerminationEvent;
import ee.jakarta.tck.ai.agent.agents.termination.ResultTerminationAgent;
import ee.jakarta.tck.ai.agent.agents.termination.ResultTerminationEvent;
import ee.jakarta.tck.ai.agent.framework.ExecutionTraceRecorder;
import ee.jakarta.tck.ai.agent.framework.ExecutionTraceRecorder.Phase;
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

/**
 * How a {@code @Decision} ends a workflow early (specification section <em>Workflow
 * Termination</em>): a negative {@code boolean}, a {@code Result} whose {@code success} is
 * false, or a {@code null} domain object stops every later phase, wherever in the chain the
 * decision sits. Termination through exceptions is covered by {@link OnExceptionTests}.
 *
 * <p>Uses {@code @TestInstance(PER_CLASS)} with inline {@code trace.reset()} because
 * {@code @BeforeEach} does not run reliably between methods of an Arquillian test
 * class inside the container.</p>
 */
@ExtendWith(ArquillianExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class WorkflowTerminationTests {

    @Deployment
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "workflowtermination.war")
                .addClasses(
                        BooleanTerminationAgent.class, BooleanTerminationEvent.class,
                        ResultTerminationAgent.class,  ResultTerminationEvent.class,
                        ObjectTerminationAgent.class,  ObjectTerminationEvent.class, ObjectTerminationDecision.class,
                        BranchingAgent.class,          BranchingEvent.class
                )
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject Event<BooleanTerminationEvent> booleanEvents;
    @Inject Event<ResultTerminationEvent>  resultEvents;
    @Inject Event<ObjectTerminationEvent>  objectEvents;
    @Inject Event<BranchingEvent>          branchingEvents;
    @Inject ExecutionTraceRecorder         trace;
    @Inject BranchingAgent                 branchingAgent;

    /**
     * {@code false} from a {@code @Decision} halts all downstream phases.
     *
     * <p>Specification section {@code workflow-termination}.
     */
    @Test
    public void booleanFalseTerminatesWorkflow() {
        trace.reset();
        booleanEvents.fire(new BooleanTerminationEvent("x"));
        assertEquals(List.of(Phase.TRIGGER, Phase.DECISION), trace.phases());
    }

    /**
     * A {@code Result} whose {@code success} is false halts all downstream phases.
     *
     * <p>Specification section {@code workflow-termination}.
     */
    @Test
    public void resultFalseTerminatesWorkflow() {
        trace.reset();
        resultEvents.fire(new ResultTerminationEvent("x"));
        assertEquals(List.of(Phase.TRIGGER, Phase.DECISION), trace.phases());
    }

    /**
     * A {@code Result} whose {@code success} is true proceeds through all downstream phases even
     * when its {@code details} are {@code null}.
     *
     * <p>Specification section {@code workflow-termination}.
     */
    @Test
    public void resultTrueWithNullDetailsProceeds() {
        trace.reset();
        resultEvents.fire(new ResultTerminationEvent("proceed"));
        assertEquals(List.of(Phase.TRIGGER, Phase.DECISION, Phase.ACTION, Phase.OUTCOME), trace.phases());
    }

    /**
     * A {@code null} domain object from a {@code @Decision} halts all downstream phases.
     *
     * <p>Specification section {@code workflow-termination}.
     */
    @Test
    public void objectNullTerminatesWorkflow() {
        trace.reset();
        objectEvents.fire(new ObjectTerminationEvent("x"));
        assertEquals(List.of(Phase.TRIGGER, Phase.DECISION), trace.phases());
    }

    /**
     * {@code false} from a later {@code @Decision}, after an earlier one proceeded, halts the
     * remaining actions and the outcome. The implementation must keep checking for termination
     * across the whole chain, not only at the first decision.
     *
     * <p>Specification section {@code workflow-termination}.
     */
    @Test
    public void falseFromLaterDecisionHaltsRemainingPhases() {
        trace.reset();
        branchingAgent.setTerminateAt(BranchingAgent.TerminationPoint.SECOND_DECISION);
        branchingEvents.fire(new BranchingEvent("test"));
        assertEquals(List.of(Phase.TRIGGER, Phase.DECISION, Phase.ACTION, Phase.DECISION), trace.phases());
    }
}
