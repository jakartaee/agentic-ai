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

import ee.jakarta.tck.ai.agent.behavior.agents.orchestration.AnchoredAgent;
import ee.jakarta.tck.ai.agent.behavior.agents.orchestration.AnchoredEvent;
import ee.jakarta.tck.ai.agent.behavior.agents.orchestration.BranchingAgent;
import ee.jakarta.tck.ai.agent.behavior.agents.orchestration.BranchingEvent;
import ee.jakarta.tck.ai.agent.behavior.agents.orchestration.IntermixedAgent;
import ee.jakarta.tck.ai.agent.behavior.agents.orchestration.IntermixedEvent;
import ee.jakarta.tck.ai.agent.behavior.agents.orchestration.LinearAgent;
import ee.jakarta.tck.ai.agent.behavior.agents.orchestration.LinearEvent;
import ee.jakarta.tck.ai.agent.behavior.agents.orchestration.MinimalistAgent;
import ee.jakarta.tck.ai.agent.behavior.agents.orchestration.MinimalistEvent;
import ee.jakarta.tck.ai.agent.behavior.agents.orchestration.OutcomeOnlyAgent;
import ee.jakarta.tck.ai.agent.behavior.agents.orchestration.OutcomeOnlyEvent;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder.Phase;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder.TraceEntry;
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
public class OrchestrationTests {

    @Deployment
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "orchestration.war")
                .addClasses(
                        MinimalistAgent.class,   MinimalistEvent.class,
                        LinearAgent.class,       LinearEvent.class,
                        IntermixedAgent.class,   IntermixedEvent.class,
                        BranchingAgent.class,    BranchingEvent.class,
                        OutcomeOnlyAgent.class,  OutcomeOnlyEvent.class,
                        AnchoredAgent.class,     AnchoredEvent.class
                )
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject Event<MinimalistEvent>  minimalistEvents;
    @Inject Event<LinearEvent>      linearEvents;
    @Inject Event<IntermixedEvent>  intermixedEvents;
    @Inject Event<BranchingEvent>   branchingEvents;
    @Inject Event<OutcomeOnlyEvent> outcomeOnlyEvents;
    @Inject Event<AnchoredEvent>    anchoredEvents;
    @Inject ExecutionTraceRecorder  trace;
    @Inject BranchingAgent          branchingAgent;

    /**
     * Minimalist pattern: an agent with {@code @Trigger} only is a valid complete workflow; firing
     * the event runs the trigger and the workflow completes.
     *
     * <p>Specification section {@code agent-lifecycle}.
     */
    @Test
    public void minimalistWorkflowCompletesWithTriggerOnly() {
        trace.reset();
        minimalistEvents.fire(new MinimalistEvent("test"));
        assertEquals(List.of(Phase.TRIGGER), trace.phases());
    }

    // -------------------------------------------------------------------------
    // Implementation-required tests — core orchestration rules
    // -------------------------------------------------------------------------
    /**
     * {@code @Trigger} is always the first phase invoked even when declared at the bottom of the
     * source file.
     *
     * <p>Specification section {@code agent-lifecycle}.
     */
    @Test
    public void triggerIsAlwaysFirstPhaseRegardlessOfPosition() {
        trace.reset();
        anchoredEvents.fire(new AnchoredEvent("test"));
        assertEquals(4, trace.phases().size());
                assertEquals(Phase.TRIGGER, trace.phases().get(0));
                assertEquals(Phase.OUTCOME, trace.phases().get(trace.phases().size() - 1));
    }
    /**
     * {@code @Decision} and {@code @Action} execute in the order given by their explicit order
     * attributes, independent of source position: AnchoredAgent orders act() before decide().
     *
     * <p>Specification section {@code agent-lifecycle}.
     */
    @Test
    public void methodsExecuteInExplicitOrder() {
        trace.reset();
        anchoredEvents.fire(new AnchoredEvent("test"));
        assertEquals(List.of(Phase.TRIGGER, Phase.ACTION, Phase.DECISION, Phase.OUTCOME), trace.phases());
        List<String> names = trace.entries().stream().map(TraceEntry::methodName).toList();
        assertEquals(List.of("onEvent", "act", "decide", "finish"), names);
    }
    /**
     * {@code @Outcome} is always the last phase invoked in a successful workflow even when
     * declared at the top of the source file — verified via AnchoredAgent where {@code @Outcome}
     * is the FIRST method declared.
     *
     * <p>Specification section {@code agent-lifecycle}.
     */
    @Test
    public void outcomeIsAlwaysLastPhaseRegardlessOfPosition() {
        trace.reset();
        anchoredEvents.fire(new AnchoredEvent("test"));
        assertEquals(Phase.OUTCOME, trace.phases().get(trace.phases().size() - 1));
    }
    /**
     * Multiple alternating {@code @Decision} and {@code @Action} phases all execute in their
     * explicit order.
     *
     * <p>Specification section {@code agent-lifecycle}.
     */
    @Test
    public void intermixedDecisionAndActionPhasesSupported() {
        trace.reset();
        intermixedEvents.fire(new IntermixedEvent("test"));
        assertEquals(List.of(Phase.TRIGGER, Phase.DECISION, Phase.ACTION,
                                 Phase.DECISION, Phase.ACTION, Phase.OUTCOME), trace.phases());
        List<String> names = trace.entries().stream().map(TraceEntry::methodName).toList();
        assertEquals(List.of("onEvent", "firstDecide", "firstAction", "secondDecide", "secondAction", "finish"), names);
    }
    /**
     * false from the FIRST {@code @Decision} in a chain immediately prevents ALL subsequent phases
     * including the trailing {@code @Outcome} anchor.
     *
     * <p>Specification section {@code agent-lifecycle}.
     */
    @Test
    public void terminationAtFirstDecisionHaltsEntirePipeline() {
        trace.reset();
        branchingAgent.setTerminateAt(BranchingAgent.TerminationPoint.FIRST_DECISION);
        branchingEvents.fire(new BranchingEvent("test"));
        assertEquals(List.of(Phase.TRIGGER, Phase.DECISION), trace.phases());
    }
    /**
     * false from a MID-CHAIN {@code @Decision} (after an upstream decision proceeded) prevents the
     * remaining {@code @Action} and the trailing {@code @Outcome} anchor — the implementation must
     * keep checking termination across the whole chain, not just at the first decision.
     *
     * <p>Specification section {@code agent-lifecycle}.
     */
    @Test
    public void terminationAtSecondDecisionHaltsRemainingPipeline() {
        trace.reset();
        branchingAgent.setTerminateAt(BranchingAgent.TerminationPoint.SECOND_DECISION);
        branchingEvents.fire(new BranchingEvent("test"));
        assertEquals(List.of(Phase.TRIGGER, Phase.DECISION, Phase.ACTION, Phase.DECISION), trace.phases());
    }

    // -------------------------------------------------------------------------
    // Implementation-required tests — composition patterns (BHV-006)
    // -------------------------------------------------------------------------
    /**
     * Linear pattern: multiple explicitly ordered {@code @Action} phases execute sequentially
     * without any {@code @Decision}.
     *
     * <p>Specification section {@code agent-lifecycle}.
     */
    @Test
    public void linearPatternSupported() {
        trace.reset();
        linearEvents.fire(new LinearEvent("test"));
        assertEquals(List.of(Phase.TRIGGER, Phase.ACTION, Phase.ACTION, Phase.ACTION, Phase.OUTCOME), trace.phases());
        List<String> names = trace.entries().stream().map(TraceEntry::methodName).toList();
        assertEquals(List.of("onEvent", "firstAction", "secondAction", "thirdAction", "finish"), names);
    }
    /**
     * Evaluative pattern: {@code @Decision} proceeds directly to {@code @Outcome} when no {@code
     * @Action} is declared.
     *
     * <p>Specification section {@code agent-lifecycle}.
     */
    @Test
    public void evaluativePatternSupported() {
        trace.reset();
        outcomeOnlyEvents.fire(new OutcomeOnlyEvent("test"));
        assertEquals(List.of(Phase.TRIGGER, Phase.DECISION, Phase.OUTCOME), trace.phases());
    }
    /**
     * Intermixed pattern: an explicitly ordered alternating {@code @Decision}/{@code @Action}
     * chain executes through {@code @Outcome}.
     *
     * <p>Specification section {@code agent-lifecycle}.
     */
    @Test
    public void intermixedPatternSupported() {
        trace.reset();
        intermixedEvents.fire(new IntermixedEvent("test"));
        assertEquals(List.of(Phase.TRIGGER, Phase.DECISION, Phase.ACTION,
                                 Phase.DECISION, Phase.ACTION, Phase.OUTCOME), trace.phases());
    }
}