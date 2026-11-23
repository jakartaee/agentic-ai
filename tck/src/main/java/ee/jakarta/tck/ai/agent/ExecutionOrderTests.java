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
import ee.jakarta.tck.ai.agent.framework.ExecutionTraceRecorder.TraceEntry;
import jakarta.ai.agent.Action;
import jakarta.ai.agent.Agent;
import jakarta.ai.agent.Decision;
import jakarta.ai.agent.Outcome;
import jakarta.ai.agent.Trigger;
import jakarta.annotation.Priority;
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
 * Execution order of the workflow phases (specification section <em>Execution
 * Order</em>). The trigger runs first and the outcome last whatever the source
 * declaration order, and decisions and actions run by {@code @Priority}, then by
 * their {@code order} attribute. Each agent declares its methods in the reverse of
 * their intended execution order, so a trace that follows the sort key proves the
 * rule rather than the declaration order.
 *
 * <p>Uses {@code @TestInstance(PER_CLASS)} with inline {@code trace.reset()} because
 * {@code @BeforeEach} does not run reliably between methods of an Arquillian test
 * class inside the container.</p>
 */
@ExtendWith(ArquillianExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ExecutionOrderTests {

    @Deployment
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "executionorder.war")
                .addClasses(
                        PhaseOrderingAgent.class,
                        PhaseOrderingEvent.class,
                        OrderAttrAgent.class,
                        OrderAttrEvent.class,
                        PriorityAgent.class,
                        PriorityEvent.class,
                        PriorityOverOrderAgent.class,
                        PriorityOverOrderEvent.class,
                        AnchoredAgent.class,
                        AnchoredEvent.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject
    Event<PhaseOrderingEvent> phaseOrderingEvents;

    @Inject
    Event<OrderAttrEvent> orderAttrEvents;

    @Inject
    Event<PriorityEvent> priorityEvents;

    @Inject
    Event<PriorityOverOrderEvent> priorityOverOrderEvents;

    @Inject
    Event<AnchoredEvent> anchoredEvents;

    @Inject
    ExecutionTraceRecorder trace;

    private List<String> recordedMethodNames() {
        return trace.entries().stream().map(TraceEntry::methodName).toList();
    }

    // -- phase order -----------------------------------------------------------

    public record PhaseOrderingEvent(String payload) {}

    @Agent
    @ApplicationScoped
    public static class PhaseOrderingAgent {

        @Inject
        ExecutionTraceRecorder trace;

        @Trigger
        public void onEvent(@Observes PhaseOrderingEvent event) {
            trace.record(Phase.TRIGGER, "onEvent", event);
        }

        @Decision(order = 1)
        public boolean decide(PhaseOrderingEvent event) {
            trace.record(Phase.DECISION, "decide", event);
            return true;
        }

        @Action(order = 2)
        public void act(PhaseOrderingEvent event) {
            trace.record(Phase.ACTION, "act", event);
        }

        @Outcome
        public void finish(PhaseOrderingEvent event) {
            trace.record(Phase.OUTCOME, "finish", event);
        }
    }

    /**
     * A workflow with one phase of each kind runs trigger, decision, action, outcome, in that
     * order.
     *
     * <p>Specification section {@code execution-order}.
     */
    @Test
    public void fullPipelineExecutesInStrictOrder() {
        trace.reset();
        phaseOrderingEvents.fire(new PhaseOrderingEvent("proceed"));

        assertEquals(List.of(Phase.TRIGGER, Phase.DECISION, Phase.ACTION, Phase.OUTCOME), trace.phases());
    }

    public record AnchoredEvent(String payload) {}

    @Agent
    @ApplicationScoped
    public static class AnchoredAgent {

        @Inject
        ExecutionTraceRecorder trace;

        @Outcome
        public void finish() {
            trace.record(Phase.OUTCOME, "finish");
        }

        @Action(order = 1)
        public void act(AnchoredEvent event) {
            trace.record(Phase.ACTION, "act", event);
        }

        @Decision(order = 2)
        public boolean decide(AnchoredEvent event) {
            trace.record(Phase.DECISION, "decide", event);
            return true;
        }

        @Trigger
        public void onEvent(@Observes AnchoredEvent event) {
            trace.record(Phase.TRIGGER, "onEvent", event);
        }
    }

    /**
     * The trigger runs first and the outcome last regardless of where they are declared, and
     * decisions and actions run in their explicit order. {@code AnchoredAgent} declares its
     * outcome first, its trigger last, and orders its action before its decision.
     *
     * <p>Specification section {@code execution-order}.
     */
    @Test
    public void phasesExecuteInExplicitOrderRegardlessOfDeclaration() {
        trace.reset();
        anchoredEvents.fire(new AnchoredEvent("test"));

        assertEquals(List.of(Phase.TRIGGER, Phase.ACTION, Phase.DECISION, Phase.OUTCOME), trace.phases());
        assertEquals(List.of("onEvent", "act", "decide", "finish"), recordedMethodNames());
    }

    // -- order attribute and @Priority -----------------------------------------

    public record OrderAttrEvent(String payload) {}

    /** Every action and decision declares {@code order}, as the consistency rule requires. */
    @Agent
    @ApplicationScoped
    public static class OrderAttrAgent {

        @Inject
        ExecutionTraceRecorder trace;

        @Trigger
        public void onEvent(@Observes OrderAttrEvent event) {
            trace.record(Phase.TRIGGER, "onEvent", event);
        }

        @Action(order = 3)
        public void third() {
            trace.record(Phase.ACTION, "third");
        }

        @Decision(order = 2)
        public boolean second() {
            trace.record(Phase.DECISION, "second");
            return true;
        }

        @Action(order = 1)
        public void first() {
            trace.record(Phase.ACTION, "first");
        }
    }

    /**
     * {@code @Action} and {@code @Decision} methods execute by ascending {@code order} attribute,
     * not source declaration order.
     *
     * <p>Specification section {@code execution-order}.
     */
    @Test
    public void orderAttributeIsHonored() {
        trace.reset();
        orderAttrEvents.fire(new OrderAttrEvent("x"));

        assertEquals(List.of("onEvent", "first", "second", "third"), recordedMethodNames());
    }

    public record PriorityEvent(String payload) {}

    /** Every action and decision declares {@code @Priority}, as the consistency rule requires. */
    @Agent
    @ApplicationScoped
    public static class PriorityAgent {

        @Inject
        ExecutionTraceRecorder trace;

        @Trigger
        public void onEvent(@Observes PriorityEvent event) {
            trace.record(Phase.TRIGGER, "onEvent", event);
        }

        @Action
        @Priority(3)
        public void third() {
            trace.record(Phase.ACTION, "third");
        }

        @Decision
        @Priority(2)
        public boolean second() {
            trace.record(Phase.DECISION, "second");
            return true;
        }

        @Action
        @Priority(1)
        public void first() {
            trace.record(Phase.ACTION, "first");
        }
    }

    /**
     * {@code @Action} and {@code @Decision} methods execute by ascending {@code @Priority} value,
     * not source declaration order.
     *
     * <p>Specification section {@code execution-order}.
     */
    @Test
    public void priorityIsHonored() {
        trace.reset();
        priorityEvents.fire(new PriorityEvent("x"));

        assertEquals(List.of("onEvent", "first", "second", "third"), recordedMethodNames());
    }

    public record PriorityOverOrderEvent(String payload) {}

    /** Each action's {@code order} and {@code @Priority} disagree, so the trace shows which one wins. */
    @Agent
    @ApplicationScoped
    public static class PriorityOverOrderAgent {

        @Inject
        ExecutionTraceRecorder trace;

        @Trigger
        public void onEvent(@Observes PriorityOverOrderEvent event) {
            trace.record(Phase.TRIGGER, "onEvent", event);
        }

        // order says this runs first, but @Priority(3) says it runs last.
        @Action(order = 1)
        @Priority(3)
        public void runsThird() {
            trace.record(Phase.ACTION, "runsThird");
        }

        // order says this runs last, but @Priority(1) says it runs first.
        @Action(order = 3)
        @Priority(1)
        public void runsFirst() {
            trace.record(Phase.ACTION, "runsFirst");
        }
    }

    /**
     * When a method declares both, {@code @Priority} takes precedence over the {@code order}
     * attribute.
     *
     * <p>Specification section {@code execution-order}.
     */
    @Test
    public void priorityTakesPrecedenceOverOrder() {
        trace.reset();
        priorityOverOrderEvents.fire(new PriorityOverOrderEvent("x"));

        assertEquals(List.of("onEvent", "runsFirst", "runsThird"), recordedMethodNames());
    }
}
