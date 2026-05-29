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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.TestInstance;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Deployed
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class OrchestrationTests {

    private static final String NO_RI =
            "Requires a Reference Implementation of the Agentic AI engine "
          + "to dispatch @Decision/@Action/@Outcome phases.";

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

    // -------------------------------------------------------------------------
    // CDI-only tests (no RI required)
    // -------------------------------------------------------------------------

    @Assertion(id = "AGENTICAI-ORCHESTRATION-BHV-006",
               section = "Workflow Composition Patterns",
               strategy = "Minimalist pattern: an agent with @Trigger only is a valid complete workflow; "
                        + "firing the event drives the trigger via CDI without any engine")
    public void minimalistWorkflowCompletesWithTriggerOnly() {
        trace.reset();
        minimalistEvents.fire(new MinimalistEvent("test"));
        assertThat(trace.phases()).containsExactly(Phase.TRIGGER);
    }

    @Assertion(id = "AGENTICAI-ORCHESTRATION-BHV-002-PRECONDITION",
               section = "Workflow Composition Patterns",
               strategy = "LinearAgent @Trigger is observed by CDI without a runtime engine")
    public void linearAgentTriggerObserved() {
        trace.reset();
        linearEvents.fire(new LinearEvent("test"));
        assertThat(trace.phases()).containsExactly(Phase.TRIGGER);
    }

    @Assertion(id = "AGENTICAI-ORCHESTRATION-BHV-004-PRECONDITION",
               section = "Workflow Composition Patterns",
               strategy = "IntermixedAgent @Trigger is observed by CDI without a runtime engine")
    public void intermixedAgentTriggerObserved() {
        trace.reset();
        intermixedEvents.fire(new IntermixedEvent("test"));
        assertThat(trace.phases()).containsExactly(Phase.TRIGGER);
    }

    @Assertion(id = "AGENTICAI-ORCHESTRATION-BHV-005-PRECONDITION",
               section = "Workflow Termination",
               strategy = "BranchingAgent @Trigger is observed by CDI without a runtime engine "
                        + "regardless of which decision is configured as the termination point")
    public void branchingAgentTriggerObserved() {
        trace.reset();
        branchingAgent.setTerminateAt(BranchingAgent.TerminationPoint.FIRST_DECISION);
        branchingEvents.fire(new BranchingEvent("test"));
        assertThat(trace.phases()).containsExactly(Phase.TRIGGER);
    }

    @Assertion(id = "AGENTICAI-ORCHESTRATION-BHV-006-EVALUATIVE-PRECONDITION",
               section = "Workflow Composition Patterns",
               strategy = "OutcomeOnlyAgent (Evaluative pattern) @Trigger is observed by CDI without a runtime engine")
    public void outcomeOnlyAgentTriggerObserved() {
        trace.reset();
        outcomeOnlyEvents.fire(new OutcomeOnlyEvent("test"));
        assertThat(trace.phases()).containsExactly(Phase.TRIGGER);
    }

    @Assertion(id = "AGENTICAI-ORCHESTRATION-BHV-001-PRECONDITION",
               section = "Workflow Composition Patterns",
               strategy = "AnchoredAgent @Trigger is observed by CDI even when declared at the bottom of the source file")
    public void anchoredAgentTriggerObserved() {
        trace.reset();
        anchoredEvents.fire(new AnchoredEvent("test"));
        assertThat(trace.phases()).containsExactly(Phase.TRIGGER);
    }

    // -------------------------------------------------------------------------
    // RI-required tests — core orchestration rules
    // -------------------------------------------------------------------------

    @Disabled(NO_RI)
    @Assertion(id = "AGENTICAI-ORCHESTRATION-BHV-001",
               section = "Workflow Composition Patterns",
               strategy = "@Trigger is always the first phase invoked even when declared at the bottom "
                        + "of the source file — verified via AnchoredAgent where @Trigger is the LAST method declared")
    public void triggerIsAlwaysFirstPhaseRegardlessOfPosition() {
        trace.reset();
        anchoredEvents.fire(new AnchoredEvent("test"));
        assertThat(trace.phases()).startsWith(Phase.TRIGGER);
    }

    @Disabled(NO_RI)
    @Assertion(id = "AGENTICAI-ORCHESTRATION-BHV-002",
               section = "@Action / Cardinality and Order",
               strategy = "@Decision and @Action execute in source-file declaration order; AnchoredAgent declares "
                        + "@Action BEFORE @Decision so an order-correct engine must invoke act() before decide()")
    public void methodsExecuteInDeclarationOrder() {
        trace.reset();
        anchoredEvents.fire(new AnchoredEvent("test"));
        assertThat(trace.phases())
                .containsExactly(Phase.TRIGGER, Phase.ACTION, Phase.DECISION, Phase.OUTCOME);
        List<String> names = trace.entries().stream().map(TraceEntry::methodName).toList();
        assertThat(names).containsExactly("onEvent", "act", "decide", "finish");
    }

    @Disabled(NO_RI)
    @Assertion(id = "AGENTICAI-ORCHESTRATION-BHV-003",
               section = "Workflow Composition Patterns",
               strategy = "@Outcome is always the last phase invoked in a successful workflow even when declared "
                        + "at the top of the source file — verified via AnchoredAgent where @Outcome is the FIRST method declared")
    public void outcomeIsAlwaysLastPhaseRegardlessOfPosition() {
        trace.reset();
        anchoredEvents.fire(new AnchoredEvent("test"));
        assertThat(trace.phases()).endsWith(Phase.OUTCOME);
    }

    @Disabled(NO_RI)
    @Assertion(id = "AGENTICAI-ORCHESTRATION-BHV-004",
               section = "Workflow Composition Patterns",
               strategy = "Multiple alternating @Decision and @Action phases all execute in declaration order")
    public void intermixedDecisionAndActionPhasesSupported() {
        trace.reset();
        intermixedEvents.fire(new IntermixedEvent("test"));
        assertThat(trace.phases())
                .containsExactly(Phase.TRIGGER, Phase.DECISION, Phase.ACTION,
                                 Phase.DECISION, Phase.ACTION, Phase.OUTCOME);
        List<String> names = trace.entries().stream().map(TraceEntry::methodName).toList();
        assertThat(names).containsExactly(
                "onEvent", "firstDecide", "firstAction", "secondDecide", "secondAction", "finish");
    }

    @Disabled(NO_RI)
    @Assertion(id = "AGENTICAI-ORCHESTRATION-BHV-005",
               section = "Workflow Termination",
               strategy = "false from the FIRST @Decision in a chain immediately prevents ALL subsequent phases "
                        + "including the trailing @Outcome anchor")
    public void terminationAtFirstDecisionHaltsEntirePipeline() {
        trace.reset();
        branchingAgent.setTerminateAt(BranchingAgent.TerminationPoint.FIRST_DECISION);
        branchingEvents.fire(new BranchingEvent("test"));
        assertThat(trace.phases()).containsExactly(Phase.TRIGGER, Phase.DECISION);
    }

    @Disabled(NO_RI)
    @Assertion(id = "AGENTICAI-ORCHESTRATION-BHV-005",
               section = "Workflow Termination",
               strategy = "false from a MID-CHAIN @Decision (after an upstream decision proceeded) prevents the "
                        + "remaining @Action and the trailing @Outcome anchor — the engine must keep checking termination "
                        + "across the whole chain, not just at the first decision")
    public void terminationAtSecondDecisionHaltsRemainingPipeline() {
        trace.reset();
        branchingAgent.setTerminateAt(BranchingAgent.TerminationPoint.SECOND_DECISION);
        branchingEvents.fire(new BranchingEvent("test"));
        assertThat(trace.phases())
                .containsExactly(Phase.TRIGGER, Phase.DECISION, Phase.ACTION, Phase.DECISION);
    }

    // -------------------------------------------------------------------------
    // RI-required tests — composition patterns (BHV-006)
    // -------------------------------------------------------------------------

    @Disabled(NO_RI)
    @Assertion(id = "AGENTICAI-ORCHESTRATION-BHV-006",
               section = "Workflow Composition Patterns",
               strategy = "Linear pattern: multiple @Action phases execute sequentially without any @Decision")
    public void linearPatternSupported() {
        trace.reset();
        linearEvents.fire(new LinearEvent("test"));
        assertThat(trace.phases())
                .containsExactly(Phase.TRIGGER, Phase.ACTION, Phase.ACTION, Phase.ACTION, Phase.OUTCOME);
        List<String> names = trace.entries().stream().map(TraceEntry::methodName).toList();
        assertThat(names).containsExactly(
                "onEvent", "firstAction", "secondAction", "thirdAction", "finish");
    }

    @Disabled(NO_RI)
    @Assertion(id = "AGENTICAI-ORCHESTRATION-BHV-006",
               section = "Workflow Composition Patterns",
               strategy = "Evaluative pattern: @Decision proceeds directly to @Outcome when no @Action is declared")
    public void evaluativePatternSupported() {
        trace.reset();
        outcomeOnlyEvents.fire(new OutcomeOnlyEvent("test"));
        assertThat(trace.phases())
                .containsExactly(Phase.TRIGGER, Phase.DECISION, Phase.OUTCOME);
    }

    @Disabled(NO_RI)
    @Assertion(id = "AGENTICAI-ORCHESTRATION-BHV-006",
               section = "Workflow Composition Patterns",
               strategy = "Intermixed pattern: alternating @Decision/@Action chain executes in declaration order through @Outcome")
    public void intermixedPatternSupported() {
        trace.reset();
        intermixedEvents.fire(new IntermixedEvent("test"));
        assertThat(trace.phases())
                .containsExactly(Phase.TRIGGER, Phase.DECISION, Phase.ACTION,
                                 Phase.DECISION, Phase.ACTION, Phase.OUTCOME);
    }
}