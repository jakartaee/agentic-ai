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

import ee.jakarta.tck.ai.agent.agents.parameterresolution.ActionOutput;
import ee.jakarta.tck.ai.agent.agents.parameterresolution.ContextInjectionAgent;
import ee.jakarta.tck.ai.agent.agents.parameterresolution.ContextInjectionEvent;
import ee.jakarta.tck.ai.agent.agents.parameterresolution.DataPropagationAgent;
import ee.jakarta.tck.ai.agent.agents.parameterresolution.DataPropagationEvent;
import ee.jakarta.tck.ai.agent.agents.parameterresolution.DecisionDetails;
import ee.jakarta.tck.ai.agent.agents.parameterresolution.DecisionOutput;
import ee.jakarta.tck.ai.agent.agents.parameterresolution.ImplicitObservesAgent;
import ee.jakarta.tck.ai.agent.agents.parameterresolution.ImplicitObservesEvent;
import ee.jakarta.tck.ai.agent.agents.parameterresolution.ResolutionCdiBean;
import ee.jakarta.tck.ai.agent.agents.parameterresolution.ResolutionOrderAgent;
import ee.jakarta.tck.ai.agent.agents.parameterresolution.ResolutionOrderEvent;
import ee.jakarta.tck.ai.agent.agents.parameterresolution.ResultDetailsAgent;
import ee.jakarta.tck.ai.agent.agents.parameterresolution.ResultDetailsEvent;
import ee.jakarta.tck.ai.agent.agents.parameterresolution.SameTypeAgent;
import ee.jakarta.tck.ai.agent.agents.parameterresolution.SameTypeEvent;
import ee.jakarta.tck.ai.agent.agents.parameterresolution.SameTypeOutput;
import ee.jakarta.tck.ai.agent.agents.parameterresolution.TriggerOutput;
import ee.jakarta.tck.ai.agent.agents.parameterresolution.VoidPhasesAgent;
import ee.jakarta.tck.ai.agent.agents.parameterresolution.VoidPhasesEvent;
import ee.jakarta.tck.ai.agent.framework.ExecutionTraceRecorder;
import ee.jakarta.tck.ai.agent.framework.ExecutionTraceRecorder.Phase;
import ee.jakarta.tck.ai.agent.framework.ExecutionTraceRecorder.TraceEntry;
import jakarta.ai.agent.LargeLanguageModel;
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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * How the parameters of phase methods are resolved (specification section <em>Parameter
 * Resolution</em> and its <em>Resolution Order</em>): the triggering event, the values earlier
 * phases returned, the details of a {@code Result}, the {@code LargeLanguageModel} and CDI
 * beans, in the order the specification defines.
 *
 * <p>Uses {@code @TestInstance(PER_CLASS)} with inline {@code trace.reset()} because
 * {@code @BeforeEach} does not run reliably between methods of an Arquillian test
 * class inside the container.</p>
 */
@ExtendWith(ArquillianExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ParameterResolutionTests {

    @Deployment
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "parameterresolution.war")
                .addClasses(
                        ContextInjectionAgent.class,  ContextInjectionEvent.class,
                        ImplicitObservesAgent.class,  ImplicitObservesEvent.class,
                        ResolutionOrderAgent.class,   ResolutionOrderEvent.class, ResolutionCdiBean.class,
                        DataPropagationAgent.class,   DataPropagationEvent.class,
                        TriggerOutput.class,          DecisionOutput.class,       ActionOutput.class,
                        ResultDetailsAgent.class,     ResultDetailsEvent.class,   DecisionDetails.class,
                        SameTypeAgent.class,          SameTypeEvent.class,        SameTypeOutput.class,
                        VoidPhasesAgent.class,        VoidPhasesEvent.class
                )
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject Event<ContextInjectionEvent>  contextInjectionEvents;
    @Inject Event<ImplicitObservesEvent>  implicitObservesEvents;
    @Inject Event<ResolutionOrderEvent>   resolutionOrderEvents;
    @Inject Event<DataPropagationEvent>   dataPropagationEvents;
    @Inject Event<ResultDetailsEvent>     resultDetailsEvents;
    @Inject Event<SameTypeEvent>          sameTypeEvents;
    @Inject Event<VoidPhasesEvent>        voidPhasesEvents;
    @Inject ExecutionTraceRecorder        trace;

    private TraceEntry entryOf(Phase phase) {
        return trace.entries().stream().filter(e -> e.phase() == phase).findFirst().orElseThrow();
    }

    /**
     * The {@code @Trigger} receives the triggering event, payload intact.
     *
     * <p>Specification section {@code agent-lifecycle}.
     */
    @Test
    public void eventPayloadIsPreservedInTrigger() {
        trace.reset();
        contextInjectionEvents.fire(new ContextInjectionEvent("my-payload"));
        assertEquals("my-payload", assertInstanceOf(ContextInjectionEvent.class, trace.entries().get(0).args()[0]).payload());
    }

    /**
     * A {@code @Trigger} whose event parameter carries no {@code @Observes} is still invoked by
     * the triggering event and runs the full workflow.
     *
     * <p>Specification section {@code agent-lifecycle}.
     */
    @Test
    public void triggerWithoutObservesRunsWorkflow() {
        trace.reset();
        implicitObservesEvents.fire(new ImplicitObservesEvent("test"));
        assertEquals(List.of(Phase.TRIGGER, Phase.ACTION, Phase.OUTCOME), trace.phases());
        assertEquals("test", assertInstanceOf(ImplicitObservesEvent.class, trace.entries().get(0).args()[0]).payload());
    }

    /**
     * The triggering event is resolved as a parameter of every downstream phase. Decision,
     * action and outcome all receive it.
     *
     * <p>Specification section {@code parameter-resolution}.
     */
    @Test
    public void triggeringEventIsAvailableInAllDownstreamPhases() {
        trace.reset();
        resolutionOrderEvents.fire(new ResolutionOrderEvent("event-payload"));
        assertEquals(List.of(Phase.TRIGGER, Phase.DECISION, Phase.ACTION, Phase.OUTCOME), trace.phases());
        trace.entries().stream()
                .filter(e -> e.phase() != Phase.TRIGGER)
                .forEach(e -> assertEquals("event-payload",
                        assertInstanceOf(ResolutionOrderEvent.class, e.args()[0]).payload()));
    }

    /**
     * The {@code LargeLanguageModel} is resolved as a method parameter of an {@code @Action},
     * and it is a working facade. Its echoed response is the rendered prompt.
     *
     * <p>Specification section {@code parameter-resolution}.
     */
    @Test
    public void llmIsResolvedAsActionParameter() {
        trace.reset();
        contextInjectionEvents.fire(new ContextInjectionEvent("payload"));
        assertEquals(List.of(Phase.TRIGGER, Phase.ACTION, Phase.OUTCOME), trace.phases());
        assertEquals("classify \"payload\"", trace.entries().get(1).args()[1]);
    }

    /**
     * Parameters are resolved in the specified order: the triggering event, then the
     * {@code LargeLanguageModel}, then CDI beans. {@code ResolutionOrderAgent}'s action receives
     * them as its first, second and third parameter.
     *
     * <p>Specification section {@code parameter-resolution}.
     */
    @Test
    public void parameterResolutionFollowsDefinedOrder() {
        trace.reset();
        resolutionOrderEvents.fire(new ResolutionOrderEvent("test"));
        assertEquals(List.of(Phase.TRIGGER, Phase.DECISION, Phase.ACTION, Phase.OUTCOME), trace.phases());
        Object[] args = entryOf(Phase.ACTION).args();
        assertInstanceOf(ResolutionOrderEvent.class, args[0]);
        assertInstanceOf(LargeLanguageModel.class, args[1]);
        assertInstanceOf(ResolutionCdiBean.class, args[2]);
    }

    /**
     * The value returned by the {@code @Trigger} is resolved as a parameter of the
     * {@code @Decision}.
     *
     * <p>Specification section {@code parameter-resolution}.
     */
    @Test
    public void triggerOutputIsInjectableInDecision() {
        trace.reset();
        dataPropagationEvents.fire(new DataPropagationEvent("input"));
        assertEquals("trigger:input", assertInstanceOf(TriggerOutput.class, trace.entries().get(1).args()[1]).fromTrigger());
    }

    /**
     * The value returned by a {@code @Decision} is resolved as a parameter of the
     * {@code @Action}.
     *
     * <p>Specification section {@code parameter-resolution}.
     */
    @Test
    public void decisionOutputIsInjectableInAction() {
        trace.reset();
        dataPropagationEvents.fire(new DataPropagationEvent("input"));
        DecisionOutput decision = assertInstanceOf(DecisionOutput.class, trace.entries().get(2).args()[1]);
        assertEquals("decision:trigger:input", decision.fromDecision());
    }

    /**
     * The {@code @Outcome} can receive the values of all preceding phases at once.
     *
     * <p>Specification section {@code parameter-resolution}.
     */
    @Test
    public void allPhaseOutputsAreInjectableInOutcome() {
        trace.reset();
        dataPropagationEvents.fire(new DataPropagationEvent("input"));
        assertEquals(List.of(Phase.TRIGGER, Phase.DECISION, Phase.ACTION, Phase.OUTCOME), trace.phases());
        Object[] outcomeArgs = trace.entries().get(3).args();
        assertInstanceOf(DataPropagationEvent.class, outcomeArgs[0]);
        assertInstanceOf(TriggerOutput.class, outcomeArgs[1]);
        assertInstanceOf(DecisionOutput.class, outcomeArgs[2]);
        assertInstanceOf(ActionOutput.class, outcomeArgs[3]);
    }

    /**
     * The non-{@code null} {@code details} of a {@code Result} returned by a {@code @Decision}
     * become workflow state, resolvable by their runtime type in a later phase.
     *
     * <p>Specification section {@code parameter-resolution}.
     */
    @Test
    public void resultDetailsAreInjectableByRuntimeType() {
        trace.reset();
        resultDetailsEvents.fire(new ResultDetailsEvent("input"));
        assertEquals(List.of(Phase.TRIGGER, Phase.DECISION, Phase.ACTION), trace.phases());
        assertEquals("details:input", assertInstanceOf(DecisionDetails.class, trace.entries().get(2).args()[0]).value());
    }

    /**
     * When more than one earlier phase returned a value of the same type, a later phase receives
     * the most recently returned one.
     *
     * <p>Specification section {@code parameter-resolution}.
     */
    @Test
    public void mostRecentlyReturnedObjectWins() {
        trace.reset();
        sameTypeEvents.fire(new SameTypeEvent("input"));
        assertEquals(List.of(Phase.TRIGGER, Phase.DECISION, Phase.ACTION, Phase.OUTCOME), trace.phases());
        assertEquals("action", assertInstanceOf(SameTypeOutput.class, trace.entries().get(3).args()[0]).marker());
    }

    /**
     * A {@code void} {@code @Trigger} adds nothing to the workflow state. The {@code @Decision}
     * receives only the triggering event.
     *
     * <p>Specification section {@code parameter-resolution}.
     */
    @Test
    public void voidTriggerAddsNothingToWorkflowState() {
        trace.reset();
        voidPhasesEvents.fire(new VoidPhasesEvent("test"));
        assertEquals(List.of(Phase.TRIGGER, Phase.DECISION, Phase.ACTION, Phase.OUTCOME), trace.phases());
        Object[] decisionArgs = trace.entries().get(1).args();
        assertEquals(1, decisionArgs.length);
        assertInstanceOf(VoidPhasesEvent.class, decisionArgs[0]);
    }

    /**
     * A {@code void} {@code @Action} leaves the workflow state intact for the {@code @Outcome}.
     *
     * <p>Specification section {@code parameter-resolution}.
     */
    @Test
    public void voidActionLeavesWorkflowStateIntact() {
        trace.reset();
        voidPhasesEvents.fire(new VoidPhasesEvent("test"));
        assertEquals(List.of(Phase.TRIGGER, Phase.DECISION, Phase.ACTION, Phase.OUTCOME), trace.phases());
        assertInstanceOf(VoidPhasesEvent.class, entryOf(Phase.OUTCOME).args()[0]);
    }
}
