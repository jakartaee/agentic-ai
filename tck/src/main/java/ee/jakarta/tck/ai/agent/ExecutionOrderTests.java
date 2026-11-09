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

import ee.jakarta.tck.ai.agent.agents.executionorder.AnchoredAgent;
import ee.jakarta.tck.ai.agent.agents.executionorder.AnchoredEvent;
import ee.jakarta.tck.ai.agent.agents.executionorder.OrderAttrAgent;
import ee.jakarta.tck.ai.agent.agents.executionorder.OrderAttrEvent;
import ee.jakarta.tck.ai.agent.agents.executionorder.PhaseOrderingAgent;
import ee.jakarta.tck.ai.agent.agents.executionorder.PhaseOrderingEvent;
import ee.jakarta.tck.ai.agent.agents.executionorder.PriorityAgent;
import ee.jakarta.tck.ai.agent.agents.executionorder.PriorityEvent;
import ee.jakarta.tck.ai.agent.agents.executionorder.PriorityOverOrderAgent;
import ee.jakarta.tck.ai.agent.agents.executionorder.PriorityOverOrderEvent;
import ee.jakarta.tck.ai.agent.framework.ExecutionTraceRecorder;
import ee.jakarta.tck.ai.agent.framework.ExecutionTraceRecorder.Phase;
import ee.jakarta.tck.ai.agent.framework.ExecutionTraceRecorder.TraceEntry;
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
                        PhaseOrderingAgent.class,      PhaseOrderingEvent.class,
                        OrderAttrAgent.class,          OrderAttrEvent.class,
                        PriorityAgent.class,           PriorityEvent.class,
                        PriorityOverOrderAgent.class,  PriorityOverOrderEvent.class,
                        AnchoredAgent.class,           AnchoredEvent.class
                )
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject Event<PhaseOrderingEvent>     phaseOrderingEvents;
    @Inject Event<OrderAttrEvent>         orderAttrEvents;
    @Inject Event<PriorityEvent>          priorityEvents;
    @Inject Event<PriorityOverOrderEvent> priorityOverOrderEvents;
    @Inject Event<AnchoredEvent>          anchoredEvents;
    @Inject ExecutionTraceRecorder        trace;

    private List<String> recordedMethodNames() {
        return trace.entries().stream().map(TraceEntry::methodName).toList();
    }

    /**
     * A workflow with one phase of each kind runs trigger, decision, action, outcome, in that
     * order.
     *
     * <p>Specification section {@code agent-lifecycle}.
     */
    @Test
    public void fullPipelineExecutesInStrictOrder() {
        trace.reset();
        phaseOrderingEvents.fire(new PhaseOrderingEvent("proceed"));
        assertEquals(List.of(Phase.TRIGGER, Phase.DECISION, Phase.ACTION, Phase.OUTCOME), trace.phases());
    }

    /**
     * The trigger runs first and the outcome last regardless of where they are declared, and
     * decisions and actions run in their explicit order. {@code AnchoredAgent} declares its
     * outcome first, its trigger last, and orders its action before its decision.
     *
     * <p>Specification section {@code agent-lifecycle}.
     */
    @Test
    public void phasesExecuteInExplicitOrderRegardlessOfDeclaration() {
        trace.reset();
        anchoredEvents.fire(new AnchoredEvent("test"));
        assertEquals(List.of(Phase.TRIGGER, Phase.ACTION, Phase.DECISION, Phase.OUTCOME), trace.phases());
        assertEquals(List.of("onEvent", "act", "decide", "finish"), recordedMethodNames());
    }

    /**
     * {@code @Action} and {@code @Decision} methods execute by ascending {@code order} attribute,
     * not source declaration order.
     *
     * <p>Specification section {@code agent-lifecycle}.
     */
    @Test
    public void orderAttributeIsHonored() {
        trace.reset();
        orderAttrEvents.fire(new OrderAttrEvent("x"));
        assertEquals(List.of("onEvent", "first", "second", "third"), recordedMethodNames());
    }

    /**
     * {@code @Action} and {@code @Decision} methods execute by ascending {@code @Priority} value,
     * not source declaration order.
     *
     * <p>Specification section {@code agent-lifecycle}.
     */
    @Test
    public void priorityIsHonored() {
        trace.reset();
        priorityEvents.fire(new PriorityEvent("x"));
        assertEquals(List.of("onEvent", "first", "second", "third"), recordedMethodNames());
    }

    /**
     * When a method declares both, {@code @Priority} takes precedence over the {@code order}
     * attribute.
     *
     * <p>Specification section {@code agent-lifecycle}.
     */
    @Test
    public void priorityTakesPrecedenceOverOrder() {
        trace.reset();
        priorityOverOrderEvents.fire(new PriorityOverOrderEvent("x"));
        assertEquals(List.of("onEvent", "runsFirst", "runsThird"), recordedMethodNames());
    }
}
