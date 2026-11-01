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

import ee.jakarta.tck.ai.agent.core.behavior.agents.orchestration.AnchoredAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.orchestration.AnchoredEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.orchestration.BranchingAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.orchestration.BranchingEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.orchestration.IntermixedAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.orchestration.IntermixedEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.orchestration.LinearAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.orchestration.LinearEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.orchestration.MinimalistAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.orchestration.MinimalistEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.orchestration.OutcomeOnlyAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.orchestration.OutcomeOnlyEvent;
import ee.jakarta.tck.ai.agent.framework.junit.anno.Assertion;
import ee.jakarta.tck.ai.agent.framework.junit.anno.Deployed;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder.Phase;
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

@Deployed
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

    @Assertion(id = "AGENTICAI-ORCHESTRATION-BHV-006",
               section = "agent-lifecycle",
               strategy = "Minimalist pattern: an agent with @Trigger only is a valid complete workflow; "
                        + "firing the event runs the trigger and the workflow completes")
    public void minimalistWorkflowCompletesWithTriggerOnly() {
        trace.reset();
        minimalistEvents.fire(new MinimalistEvent("test"));
        assertEquals(List.of(Phase.TRIGGER), trace.phases());
    }

    // -------------------------------------------------------------------------
    // Implementation-required tests — core orchestration rules
    // -------------------------------------------------------------------------
    @Assertion(id = "AGENTICAI-ORCHESTRATION-BHV-001",
               section = "agent-lifecycle",
               strategy = "@Trigger is always the first phase invoked even when declared at the bottom "
                        + "of the source file")
    public void triggerIsAlwaysFirstPhaseRegardlessOfPosition() {
        trace.reset();
        anchoredEvents.fire(new AnchoredEvent("test"));
        assertEquals(4, trace.phases().size());
                assertEquals(Phase.TRIGGER, trace.phases().get(0));
                assertEquals(Phase.OUTCOME, trace.phases().get(trace.phases().size() - 1));
    }
    @Assertion(id = "AGENTICAI-ORCHESTRATION-BHV-002",
               section = "agent-lifecycle",
               strategy = "@Decision and @Action execute in the order given by their explicit order attributes, "
                        + "independent of source position: AnchoredAgent orders act() before decide()")
    public void methodsExecuteInExplicitOrder() {
        trace.reset();
        anchoredEvents.fire(new AnchoredEvent("test"));
        assertEquals(List.of(Phase.TRIGGER, Phase.ACTION, Phase.DECISION, Phase.OUTCOME), trace.phases());
        List<String> names = trace.entries().stream().map(TraceEntry::methodName).toList();
        assertEquals(List.of("onEvent", "act", "decide", "finish"), names);
    }
    @Assertion(id = "AGENTICAI-ORCHESTRATION-BHV-003",
               section = "agent-lifecycle",
               strategy = "@Outcome is always the last phase invoked in a successful workflow even when declared "
                        + "at the top of the source file — verified via AnchoredAgent where @Outcome is the FIRST method declared")
    public void outcomeIsAlwaysLastPhaseRegardlessOfPosition() {
        trace.reset();
        anchoredEvents.fire(new AnchoredEvent("test"));
        assertEquals(Phase.OUTCOME, trace.phases().get(trace.phases().size() - 1));
    }
    @Assertion(id = "AGENTICAI-ORCHESTRATION-BHV-004",
               section = "agent-lifecycle",
               strategy = "Multiple alternating @Decision and @Action phases all execute in their explicit order")
    public void intermixedDecisionAndActionPhasesSupported() {
        trace.reset();
        intermixedEvents.fire(new IntermixedEvent("test"));
        assertEquals(List.of(Phase.TRIGGER, Phase.DECISION, Phase.ACTION,
                                 Phase.DECISION, Phase.ACTION, Phase.OUTCOME), trace.phases());
        List<String> names = trace.entries().stream().map(TraceEntry::methodName).toList();
        assertEquals(List.of("onEvent", "firstDecide", "firstAction", "secondDecide", "secondAction", "finish"), names);
    }
    @Assertion(id = "AGENTICAI-ORCHESTRATION-BHV-005",
               section = "agent-lifecycle",
               strategy = "false from the FIRST @Decision in a chain immediately prevents ALL subsequent phases "
                        + "including the trailing @Outcome anchor")
    public void terminationAtFirstDecisionHaltsEntirePipeline() {
        trace.reset();
        branchingAgent.setTerminateAt(BranchingAgent.TerminationPoint.FIRST_DECISION);
        branchingEvents.fire(new BranchingEvent("test"));
        assertEquals(List.of(Phase.TRIGGER, Phase.DECISION), trace.phases());
    }
    @Assertion(id = "AGENTICAI-ORCHESTRATION-BHV-005",
               section = "agent-lifecycle",
               strategy = "false from a MID-CHAIN @Decision (after an upstream decision proceeded) prevents the "
                        + "remaining @Action and the trailing @Outcome anchor — the implementation must keep checking termination "
                        + "across the whole chain, not just at the first decision")
    public void terminationAtSecondDecisionHaltsRemainingPipeline() {
        trace.reset();
        branchingAgent.setTerminateAt(BranchingAgent.TerminationPoint.SECOND_DECISION);
        branchingEvents.fire(new BranchingEvent("test"));
        assertEquals(List.of(Phase.TRIGGER, Phase.DECISION, Phase.ACTION, Phase.DECISION), trace.phases());
    }

    // -------------------------------------------------------------------------
    // Implementation-required tests — composition patterns (BHV-006)
    // -------------------------------------------------------------------------
    @Assertion(id = "AGENTICAI-ORCHESTRATION-BHV-006",
               section = "agent-lifecycle",
               strategy = "Linear pattern: multiple explicitly ordered @Action phases execute sequentially without any @Decision")
    public void linearPatternSupported() {
        trace.reset();
        linearEvents.fire(new LinearEvent("test"));
        assertEquals(List.of(Phase.TRIGGER, Phase.ACTION, Phase.ACTION, Phase.ACTION, Phase.OUTCOME), trace.phases());
        List<String> names = trace.entries().stream().map(TraceEntry::methodName).toList();
        assertEquals(List.of("onEvent", "firstAction", "secondAction", "thirdAction", "finish"), names);
    }
    @Assertion(id = "AGENTICAI-ORCHESTRATION-BHV-006",
               section = "agent-lifecycle",
               strategy = "Evaluative pattern: @Decision proceeds directly to @Outcome when no @Action is declared")
    public void evaluativePatternSupported() {
        trace.reset();
        outcomeOnlyEvents.fire(new OutcomeOnlyEvent("test"));
        assertEquals(List.of(Phase.TRIGGER, Phase.DECISION, Phase.OUTCOME), trace.phases());
    }
    @Assertion(id = "AGENTICAI-ORCHESTRATION-BHV-006",
               section = "agent-lifecycle",
               strategy = "Intermixed pattern: an explicitly ordered alternating @Decision/@Action chain executes through @Outcome")
    public void intermixedPatternSupported() {
        trace.reset();
        intermixedEvents.fire(new IntermixedEvent("test"));
        assertEquals(List.of(Phase.TRIGGER, Phase.DECISION, Phase.ACTION,
                                 Phase.DECISION, Phase.ACTION, Phase.OUTCOME), trace.phases());
    }
}