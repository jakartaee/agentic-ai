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

import ee.jakarta.tck.ai.agent.core.behavior.agents.ordering.OrderAttrAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.ordering.OrderAttrEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.ordering.PriorityAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.ordering.PriorityEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.ordering.PriorityOverOrderAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.ordering.PriorityOverOrderEvent;
import ee.jakarta.tck.ai.agent.framework.junit.anno.Assertion;
import ee.jakarta.tck.ai.agent.framework.junit.anno.Deployed;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder.TraceEntry;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.TestInstance;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies the execution-order precedence rules for {@code @Action}/{@code @Decision}
 * methods (see the spec, {@code [[agent-lifecycle]]}, <em>Execution Order</em>):
 * {@code @Priority} &gt; {@code order} &gt; source declaration order.
 *
 * <p>Each agent declares its phase methods in the reverse of their intended
 * execution order, so a trace that follows the sort key (not source order)
 * proves the rule is honored. Ordering is performed by the implementation when
 * it dispatches phases, so the behavioral assertions are
 * a deployment-error test.</p>
 *
 * <p>Note on reset: in {@code @Deployed} classes {@code @BeforeEach} does not run
 * reliably between methods inside the container, so each test resets the trace
 * inline.</p>
 */
@Deployed
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class OrderingPrecedenceTests {

    @Deployment
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "ordering.war")
                .addClasses(
                        OrderAttrAgent.class,          OrderAttrEvent.class,
                        PriorityAgent.class,           PriorityEvent.class,
                        PriorityOverOrderAgent.class,  PriorityOverOrderEvent.class
                )
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject Event<OrderAttrEvent>         orderAttrEvents;
    @Inject Event<PriorityEvent>          priorityEvents;
    @Inject Event<PriorityOverOrderEvent> priorityOverOrderEvents;
    @Inject ExecutionTraceRecorder        trace;

    /** Returns the recorded method names in execution order. */
    private List<String> recordedMethodNames() {
        return trace.entries().stream().map(TraceEntry::methodName).toList();
    }

    // ── order attribute is honored ───────────────────────────────────────────
    @Assertion(id = "AGENTICAI-ORDER-004",
               section = "agent-lifecycle",
               strategy = "@Action/@Decision methods execute by ascending order attribute, not source declaration order")
    public void orderAttributeIsHonored() {
        trace.reset();
        orderAttrEvents.fire(new OrderAttrEvent("x"));
        assertEquals(List.of("onEvent", "first", "second", "third"), recordedMethodNames());
    }

    // ── @Priority is honored ─────────────────────────────────────────────────
    @Assertion(id = "AGENTICAI-ORDER-005",
               section = "agent-lifecycle",
               strategy = "@Action/@Decision methods execute by ascending @Priority value, not source declaration order")
    public void priorityIsHonored() {
        trace.reset();
        priorityEvents.fire(new PriorityEvent("x"));
        assertEquals(List.of("onEvent", "first", "second", "third"), recordedMethodNames());
    }

    // ── @Priority takes precedence over order ────────────────────────────────
    @Assertion(id = "AGENTICAI-ORDER-006",
               section = "agent-lifecycle",
               strategy = "When a method declares both, @Priority takes precedence over the order attribute")
    public void priorityTakesPrecedenceOverOrder() {
        trace.reset();
        priorityOverOrderEvents.fire(new PriorityOverOrderEvent("x"));
        assertEquals(List.of("onEvent", "runsFirst", "runsThird"), recordedMethodNames());
    }
}