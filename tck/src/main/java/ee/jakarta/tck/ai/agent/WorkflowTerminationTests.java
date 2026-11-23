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

import ee.jakarta.tck.ai.agent.framework.ExecutionTraceRecorder;
import ee.jakarta.tck.ai.agent.framework.ExecutionTraceRecorder.Phase;
import jakarta.ai.agent.Action;
import jakarta.ai.agent.Agent;
import jakarta.ai.agent.Decision;
import jakarta.ai.agent.Outcome;
import jakarta.ai.agent.Result;
import jakarta.ai.agent.Trigger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
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

    // -- boolean ---------------------------------------------------------------

    public record BooleanTerminationEvent(String payload) {}

    @Agent
    @ApplicationScoped
    public static class BooleanTerminationAgent {

        @Inject ExecutionTraceRecorder trace;

        @Trigger
        public void onEvent(@Observes BooleanTerminationEvent event) {
            trace.record(Phase.TRIGGER, "onEvent", event);
        }

        @Decision(order = 1)
        public boolean decide(BooleanTerminationEvent event) {
            trace.record(Phase.DECISION, "decide", event);
            return false;
        }

        @Action(order = 2)
        public void act(BooleanTerminationEvent event) {
            trace.record(Phase.ACTION, "act", event);
        }

        @Outcome
        public void finish() {
            trace.record(Phase.OUTCOME, "finish");
        }
    }

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

    // -- Result ----------------------------------------------------------------

    public record ResultTerminationEvent(String payload) {}

    /** Proceeds with {@code null} details when the payload is {@code "proceed"}, and terminates otherwise. */
    @Agent
    @ApplicationScoped
    public static class ResultTerminationAgent {

        @Inject ExecutionTraceRecorder trace;

        @Trigger
        public void onEvent(@Observes ResultTerminationEvent event) {
            trace.record(Phase.TRIGGER, "onEvent", event);
        }

        @Decision(order = 1)
        public Result decide(ResultTerminationEvent event) {
            trace.record(Phase.DECISION, "decide", event);
            return "proceed".equals(event.payload())
                    ? new Result(true, null)
                    : new Result(false, "deliberately terminated");
        }

        @Action(order = 2)
        public void act(ResultTerminationEvent event) {
            trace.record(Phase.ACTION, "act", event);
        }

        @Outcome
        public void finish() {
            trace.record(Phase.OUTCOME, "finish");
        }
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

    // -- domain object ---------------------------------------------------------

    public record ObjectTerminationEvent(String payload) {}

    public record ObjectTerminationDecision(String reason) {}

    @Agent
    @ApplicationScoped
    public static class ObjectTerminationAgent {

        @Inject ExecutionTraceRecorder trace;

        @Trigger
        public void onEvent(@Observes ObjectTerminationEvent event) {
            trace.record(Phase.TRIGGER, "onEvent", event);
        }

        @Decision(order = 1)
        public ObjectTerminationDecision decide(ObjectTerminationEvent event) {
            trace.record(Phase.DECISION, "decide", event);
            return null;
        }

        @Action(order = 2)
        public void act(ObjectTerminationEvent event) {
            trace.record(Phase.ACTION, "act", event);
        }

        @Outcome
        public void finish() {
            trace.record(Phase.OUTCOME, "finish");
        }
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

    // -- later decision --------------------------------------------------------

    public record BranchingEvent(String payload) {}

    /** Two decisions around an action. The test chooses which decision returns {@code false}. */
    @Agent
    @ApplicationScoped
    public static class BranchingAgent {

        public enum TerminationPoint { FIRST_DECISION, SECOND_DECISION }

        @Inject ExecutionTraceRecorder trace;
        private volatile TerminationPoint terminateAt = TerminationPoint.FIRST_DECISION;

        public void setTerminateAt(TerminationPoint terminateAt) {
            this.terminateAt = terminateAt;
        }

        @Trigger
        public void onEvent(@Observes BranchingEvent event) {
            trace.record(Phase.TRIGGER, "onEvent", event);
        }

        @Decision(order = 1)
        public boolean firstDecide(BranchingEvent event) {
            trace.record(Phase.DECISION, "firstDecide", event);
            return terminateAt != TerminationPoint.FIRST_DECISION;
        }

        @Action(order = 2)
        public void firstAction(BranchingEvent event) {
            trace.record(Phase.ACTION, "firstAction", event);
        }

        @Decision(order = 3)
        public boolean secondDecide(BranchingEvent event) {
            trace.record(Phase.DECISION, "secondDecide", event);
            return terminateAt != TerminationPoint.SECOND_DECISION;
        }

        @Action(order = 4)
        public void secondAction(BranchingEvent event) {
            trace.record(Phase.ACTION, "secondAction", event);
        }

        @Outcome
        public void finish() {
            trace.record(Phase.OUTCOME, "finish");
        }
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
