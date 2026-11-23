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
import jakarta.ai.agent.LargeLanguageModel;
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
                        LlmParameterAgent.class,
                        LlmParameterEvent.class,
                        ImplicitObservesAgent.class,
                        ImplicitObservesEvent.class,
                        ResolutionOrderAgent.class,
                        ResolutionOrderEvent.class,
                        ResolutionCdiBean.class,
                        DataPropagationAgent.class,
                        DataPropagationEvent.class,
                        TriggerOutput.class,
                        DecisionOutput.class,
                        ActionOutput.class,
                        ResultDetailsAgent.class,
                        ResultDetailsEvent.class,
                        DecisionDetails.class,
                        SameTypeAgent.class,
                        SameTypeEvent.class,
                        SameTypeOutput.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject
    Event<LlmParameterEvent> llmParameterEvents;

    @Inject
    Event<ImplicitObservesEvent> implicitObservesEvents;

    @Inject
    Event<ResolutionOrderEvent> resolutionOrderEvents;

    @Inject
    Event<DataPropagationEvent> dataPropagationEvents;

    @Inject
    Event<ResultDetailsEvent> resultDetailsEvents;

    @Inject
    Event<SameTypeEvent> sameTypeEvents;

    @Inject
    ExecutionTraceRecorder trace;

    public record LlmParameterEvent(String payload) {}

    @Agent
    @ApplicationScoped
    public static class LlmParameterAgent {

        @Inject
        ExecutionTraceRecorder trace;

        @Trigger
        public void onEvent(@Observes LlmParameterEvent event, LargeLanguageModel llm) {
            trace.record(Phase.TRIGGER, "onEvent", event, llm.query("classify {}", event.payload()));
        }

        @Action
        public void act(LlmParameterEvent event, LargeLanguageModel llm) {
            trace.record(Phase.ACTION, "act", event, llm.query("classify {}", event.payload()));
        }

        @Outcome
        public void finish(LlmParameterEvent event) {
            trace.record(Phase.OUTCOME, "finish", event);
        }
    }

    /**
     * The {@code @Trigger} receives the triggering event, payload intact.
     *
     * <p>Specification section {@code trigger-parameters}.
     */
    @Test
    public void eventPayloadIsPreservedInTrigger() {
        trace.reset();
        llmParameterEvents.fire(new LlmParameterEvent("my-payload"));

        assertEquals(
                "my-payload",
                assertInstanceOf(
                                LlmParameterEvent.class,
                                trace.entry(Phase.TRIGGER).args()[0])
                        .payload());
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
        llmParameterEvents.fire(new LlmParameterEvent("payload"));

        assertEquals(List.of(Phase.TRIGGER, Phase.ACTION, Phase.OUTCOME), trace.phases());
        assertEquals("classify \"payload\"", trace.entry(Phase.ACTION).args()[1]);
    }

    public record ImplicitObservesEvent(String payload) {}

    /** Declares its trigger without {@code @Observes}, the optional form the specification's own examples use. */
    @Agent
    @ApplicationScoped
    public static class ImplicitObservesAgent {

        @Inject
        ExecutionTraceRecorder trace;

        @Trigger
        public void onEvent(ImplicitObservesEvent event) {
            trace.record(Phase.TRIGGER, "onEvent", event);
        }

        @Action
        public void act(ImplicitObservesEvent event) {
            trace.record(Phase.ACTION, "act", event);
        }

        @Outcome
        public void finish(ImplicitObservesEvent event) {
            trace.record(Phase.OUTCOME, "finish", event);
        }
    }

    /**
     * A {@code @Trigger} whose event parameter carries no {@code @Observes} is still invoked by
     * the triggering event and runs the full workflow.
     *
     * <p>Specification section {@code trigger-invocation}.
     */
    @Test
    public void triggerWithoutObservesRunsWorkflow() {
        trace.reset();
        implicitObservesEvents.fire(new ImplicitObservesEvent("test"));

        assertEquals(List.of(Phase.TRIGGER, Phase.ACTION, Phase.OUTCOME), trace.phases());
        assertEquals(
                "test",
                assertInstanceOf(
                                ImplicitObservesEvent.class,
                                trace.entry(Phase.TRIGGER).args()[0])
                        .payload());
    }

    public record ResolutionOrderEvent(String payload) {}

    @ApplicationScoped
    public static class ResolutionCdiBean {}

    @Agent
    @ApplicationScoped
    public static class ResolutionOrderAgent {

        @Inject
        ExecutionTraceRecorder trace;

        @Trigger
        public void onEvent(@Observes ResolutionOrderEvent event) {
            trace.record(Phase.TRIGGER, "onEvent", event);
        }

        @Decision(order = 1)
        public boolean decide(ResolutionOrderEvent event, LargeLanguageModel llm) {
            trace.record(Phase.DECISION, "decide", event, llm);
            return true;
        }

        @Action(order = 2)
        public void process(ResolutionOrderEvent event, LargeLanguageModel llm, ResolutionCdiBean service) {
            trace.record(Phase.ACTION, "process", event, llm, service);
        }

        @Outcome
        public void finish(ResolutionOrderEvent event, ResolutionCdiBean service) {
            trace.record(Phase.OUTCOME, "finish", event, service);
        }
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
                .forEach(e -> assertEquals(
                        "event-payload",
                        assertInstanceOf(ResolutionOrderEvent.class, e.args()[0])
                                .payload()));
    }

    /**
     * A parameter that matches neither the triggering event, an earlier phase's result nor the
     * {@code LargeLanguageModel} is resolved by CDI injection. {@code ResolutionOrderAgent}'s action
     * receives the {@code ResolutionCdiBean} bean.
     *
     * <p>Specification section {@code resolution-order}.
     */
    @Test
    public void cdiBeanIsResolvedAsParameter() {
        trace.reset();
        resolutionOrderEvents.fire(new ResolutionOrderEvent("test"));

        assertEquals(List.of(Phase.TRIGGER, Phase.DECISION, Phase.ACTION, Phase.OUTCOME), trace.phases());
        assertInstanceOf(ResolutionCdiBean.class, trace.entry(Phase.ACTION).args()[2]);
    }

    public record DataPropagationEvent(String input) {}

    public record TriggerOutput(String fromTrigger) {}

    public record DecisionOutput(String fromDecision) {}

    public record ActionOutput(String fromAction) {}

    @Agent
    @ApplicationScoped
    public static class DataPropagationAgent {

        @Inject
        ExecutionTraceRecorder trace;

        @Inject
        LargeLanguageModel llm;

        @Trigger
        public TriggerOutput onEvent(@Observes DataPropagationEvent event) {
            trace.record(Phase.TRIGGER, "onEvent", event);
            return new TriggerOutput("trigger:" + event.input());
        }

        @Decision(order = 1)
        public DecisionOutput decide(DataPropagationEvent event, TriggerOutput trigger) {
            trace.record(Phase.DECISION, "decide", event, trigger);
            String response = llm.query(trigger.fromTrigger());
            return new DecisionOutput("decision:" + response);
        }

        @Action(order = 2)
        public ActionOutput act(TriggerOutput trigger, DecisionOutput decision) {
            trace.record(Phase.ACTION, "act", trigger, decision);
            return new ActionOutput("action:" + trigger.fromTrigger() + "+" + decision.fromDecision());
        }

        @Outcome
        public void finish(
                DataPropagationEvent event, TriggerOutput trigger, DecisionOutput decision, ActionOutput action) {
            trace.record(Phase.OUTCOME, "finish", event, trigger, decision, action);
        }
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

        assertEquals(
                "trigger:input",
                assertInstanceOf(
                                TriggerOutput.class, trace.entry(Phase.DECISION).args()[1])
                        .fromTrigger());
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

        DecisionOutput decision =
                assertInstanceOf(DecisionOutput.class, trace.entry(Phase.ACTION).args()[1]);
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
        Object[] outcomeArgs = trace.entry(Phase.OUTCOME).args();
        assertInstanceOf(DataPropagationEvent.class, outcomeArgs[0]);
        assertInstanceOf(TriggerOutput.class, outcomeArgs[1]);
        assertInstanceOf(DecisionOutput.class, outcomeArgs[2]);
        assertInstanceOf(ActionOutput.class, outcomeArgs[3]);
    }

    public record ResultDetailsEvent(String input) {}

    /** Carried as the {@code details} of the {@code Result} that the decision below returns. */
    public record DecisionDetails(String value) {}

    /**
     * Its {@code @Decision} returns {@code new Result(true, new DecisionDetails(...))}, and its
     * {@code @Action} takes the {@link DecisionDetails}, not the {@link Result} wrapper.
     */
    @Agent
    @ApplicationScoped
    public static class ResultDetailsAgent {

        @Inject
        ExecutionTraceRecorder trace;

        @Trigger
        public void onEvent(@Observes ResultDetailsEvent event) {
            trace.record(Phase.TRIGGER, "onEvent", event);
        }

        @Decision(order = 1)
        public Result decide(ResultDetailsEvent event) {
            trace.record(Phase.DECISION, "decide", event);
            return new Result(true, new DecisionDetails("details:" + event.input()));
        }

        @Action(order = 2)
        public void act(DecisionDetails details) {
            trace.record(Phase.ACTION, "act", details);
        }
    }

    /**
     * The non-{@code null} {@code details} of a {@code Result} returned by a {@code @Decision}
     * become workflow state, resolvable by their runtime type in a later phase.
     *
     * <p>Specification section {@code decision-return-type}.
     */
    @Test
    public void resultDetailsAreInjectableByRuntimeType() {
        trace.reset();
        resultDetailsEvents.fire(new ResultDetailsEvent("input"));

        assertEquals(List.of(Phase.TRIGGER, Phase.DECISION, Phase.ACTION), trace.phases());
        assertEquals(
                "details:input",
                assertInstanceOf(
                                DecisionDetails.class, trace.entry(Phase.ACTION).args()[0])
                        .value());
    }

    public record SameTypeEvent(String input) {}

    /** Returned by both the decision and the action below. The marker names the phase that produced it. */
    public record SameTypeOutput(String marker) {}

    /**
     * Its {@code @Decision} and {@code @Action} both return a {@link SameTypeOutput}, tagged
     * {@code "decision"} and {@code "action"}. The {@code @Outcome} takes a single one, which
     * must be the {@code "action"} instance because it was returned last.
     */
    @Agent
    @ApplicationScoped
    public static class SameTypeAgent {

        @Inject
        ExecutionTraceRecorder trace;

        @Trigger
        public void onEvent(@Observes SameTypeEvent event) {
            trace.record(Phase.TRIGGER, "onEvent", event);
        }

        @Decision(order = 1)
        public SameTypeOutput decide(SameTypeEvent event) {
            trace.record(Phase.DECISION, "decide", event);
            return new SameTypeOutput("decision");
        }

        @Action(order = 2)
        public SameTypeOutput act(SameTypeEvent event) {
            trace.record(Phase.ACTION, "act", event);
            return new SameTypeOutput("action");
        }

        @Outcome
        public void finish(SameTypeOutput latest) {
            trace.record(Phase.OUTCOME, "finish", latest);
        }
    }

    /**
     * When more than one earlier phase returned a value of the same type, a later phase receives
     * the most recently returned one.
     *
     * <p>Specification section {@code resolution-order}.
     */
    @Test
    public void mostRecentlyReturnedObjectWins() {
        trace.reset();
        sameTypeEvents.fire(new SameTypeEvent("input"));

        assertEquals(List.of(Phase.TRIGGER, Phase.DECISION, Phase.ACTION, Phase.OUTCOME), trace.phases());
        assertEquals(
                "action",
                assertInstanceOf(
                                SameTypeOutput.class, trace.entry(Phase.OUTCOME).args()[0])
                        .marker());
    }
}
