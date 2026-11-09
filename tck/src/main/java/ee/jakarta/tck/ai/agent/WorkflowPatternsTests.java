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

import ee.jakarta.tck.ai.agent.agents.workflowpatterns.IntermixedAgent;
import ee.jakarta.tck.ai.agent.agents.workflowpatterns.IntermixedEvent;
import ee.jakarta.tck.ai.agent.agents.workflowpatterns.LinearAgent;
import ee.jakarta.tck.ai.agent.agents.workflowpatterns.LinearEvent;
import ee.jakarta.tck.ai.agent.agents.workflowpatterns.MinimalistAgent;
import ee.jakarta.tck.ai.agent.agents.workflowpatterns.MinimalistEvent;
import ee.jakarta.tck.ai.agent.agents.workflowpatterns.NoDecisionAgent;
import ee.jakarta.tck.ai.agent.agents.workflowpatterns.NoDecisionEvent;
import ee.jakarta.tck.ai.agent.agents.workflowpatterns.NoOutcomeAgent;
import ee.jakarta.tck.ai.agent.agents.workflowpatterns.NoOutcomeEvent;
import ee.jakarta.tck.ai.agent.agents.workflowpatterns.OutcomeOnlyAgent;
import ee.jakarta.tck.ai.agent.agents.workflowpatterns.OutcomeOnlyEvent;
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
 * The workflow shapes the specification allows (section <em>Workflow Patterns</em>, and the
 * cardinalities under <em>Workflow Phases and Annotations</em>): every phase but the trigger
 * is optional, actions may be sequential, and decisions and actions may be intermixed.
 *
 * <p>Uses {@code @TestInstance(PER_CLASS)} with inline {@code trace.reset()} because
 * {@code @BeforeEach} does not run reliably between methods of an Arquillian test
 * class inside the container.</p>
 */
@ExtendWith(ArquillianExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class WorkflowPatternsTests {

    @Deployment
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "workflowpatterns.war")
                .addClasses(
                        MinimalistAgent.class,   MinimalistEvent.class,
                        NoDecisionAgent.class,   NoDecisionEvent.class,
                        NoOutcomeAgent.class,    NoOutcomeEvent.class,
                        LinearAgent.class,       LinearEvent.class,
                        OutcomeOnlyAgent.class,  OutcomeOnlyEvent.class,
                        IntermixedAgent.class,   IntermixedEvent.class
                )
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject Event<MinimalistEvent>  minimalistEvents;
    @Inject Event<NoDecisionEvent>  noDecisionEvents;
    @Inject Event<NoOutcomeEvent>   noOutcomeEvents;
    @Inject Event<LinearEvent>      linearEvents;
    @Inject Event<OutcomeOnlyEvent> outcomeOnlyEvents;
    @Inject Event<IntermixedEvent>  intermixedEvents;
    @Inject ExecutionTraceRecorder  trace;

    private List<String> recordedMethodNames() {
        return trace.entries().stream().map(TraceEntry::methodName).toList();
    }

    /**
     * An agent with only a {@code @Trigger} is a complete workflow. Firing the event runs the
     * trigger and the workflow completes.
     *
     * <p>Specification section {@code agent-lifecycle}.
     */
    @Test
    public void triggerOnlyWorkflowCompletes() {
        trace.reset();
        minimalistEvents.fire(new MinimalistEvent("test"));
        assertEquals(List.of(Phase.TRIGGER), trace.phases());
    }

    /**
     * A workflow without a {@code @Decision} runs trigger, action, outcome.
     *
     * <p>Specification section {@code agent-lifecycle}.
     */
    @Test
    public void workflowWithoutDecisionRunsActions() {
        trace.reset();
        noDecisionEvents.fire(new NoDecisionEvent("test"));
        assertEquals(List.of(Phase.TRIGGER, Phase.ACTION, Phase.OUTCOME), trace.phases());
    }

    /**
     * A workflow without an {@code @Outcome} completes after its last action.
     *
     * <p>Specification section {@code agent-lifecycle}.
     */
    @Test
    public void workflowWithoutOutcomeCompletes() {
        trace.reset();
        noOutcomeEvents.fire(new NoOutcomeEvent("test"));
        assertEquals(List.of(Phase.TRIGGER, Phase.DECISION, Phase.ACTION), trace.phases());
    }

    /**
     * Several explicitly ordered {@code @Action} phases execute sequentially without any
     * {@code @Decision}.
     *
     * <p>Specification section {@code agent-lifecycle}.
     */
    @Test
    public void sequentialActionsExecuteInOrder() {
        trace.reset();
        linearEvents.fire(new LinearEvent("test"));
        assertEquals(List.of(Phase.TRIGGER, Phase.ACTION, Phase.ACTION, Phase.ACTION, Phase.OUTCOME), trace.phases());
        assertEquals(List.of("onEvent", "firstAction", "secondAction", "thirdAction", "finish"), recordedMethodNames());
    }

    /**
     * A {@code @Decision} proceeds directly to the {@code @Outcome} when no {@code @Action} is
     * declared.
     *
     * <p>Specification section {@code agent-lifecycle}.
     */
    @Test
    public void decisionProceedsDirectlyToOutcome() {
        trace.reset();
        outcomeOnlyEvents.fire(new OutcomeOnlyEvent("test"));
        assertEquals(List.of(Phase.TRIGGER, Phase.DECISION, Phase.OUTCOME), trace.phases());
    }

    /**
     * Alternating {@code @Decision} and {@code @Action} phases all execute, in their explicit
     * order, through to the {@code @Outcome}.
     *
     * <p>Specification section {@code agent-lifecycle}.
     */
    @Test
    public void intermixedDecisionsAndActionsExecuteInOrder() {
        trace.reset();
        intermixedEvents.fire(new IntermixedEvent("test"));
        assertEquals(List.of(Phase.TRIGGER, Phase.DECISION, Phase.ACTION,
                             Phase.DECISION, Phase.ACTION, Phase.OUTCOME), trace.phases());
        assertEquals(List.of("onEvent", "firstDecide", "firstAction", "secondDecide", "secondAction", "finish"),
                     recordedMethodNames());
    }
}
