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
import jakarta.ai.agent.OnException;
import jakarta.ai.agent.Outcome;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Exception handling with {@code @OnException} (specification sections <em>@OnException</em>
 * and <em>Error Handling</em>): which handler runs, what it receives, what happens when it
 * returns normally or rethrows, and what happens when there is none.
 *
 * <p>Uses {@code @TestInstance(PER_CLASS)} with inline {@code trace.reset()} because
 * {@code @BeforeEach} does not run reliably between methods of an Arquillian test
 * class inside the container.</p>
 */
@ExtendWith(ArquillianExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class OnExceptionTests {

    @Deployment
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "onexception.war")
                .addClasses(
                        AgentDomainException.class,   SpecificDomainException.class,
                        RecoveryAgent.class,           RecoveryEvent.class,
                        PropagationAgent.class,        PropagationEvent.class,
                        HierarchyAgent.class,          HierarchyEvent.class,
                        RecursiveGuardAgent.class,     RecursiveGuardEvent.class,
                        PhaseFailureAgent.class,       PhaseFailureEvent.class,
                        NoHandlerAgent.class,          NoHandlerEvent.class,
                        StatefulHandlerAgent.class,    StatefulHandlerEvent.class
                )
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject Event<RecoveryEvent>        recoveryEvents;
    @Inject Event<PropagationEvent>     propagationEvents;
    @Inject Event<HierarchyEvent>       hierarchyEvents;
    @Inject Event<RecursiveGuardEvent>  recursiveGuardEvents;
    @Inject Event<PhaseFailureEvent>    phaseFailureEvents;
    @Inject Event<NoHandlerEvent>       noHandlerEvents;
    @Inject Event<StatefulHandlerEvent> statefulHandlerEvents;
    @Inject ExecutionTraceRecorder      trace;
    @Inject PhaseFailureAgent           phaseFailureAgent;

    public static class AgentDomainException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public AgentDomainException(String message) {
            super(message);
        }
    }

    // The container may rethrow the agent's exception as is or wrapped; the spec only says it propagates.
    private static AgentDomainException agentExceptionIn(Throwable thrown) {
        for (Throwable t = thrown; t != null; t = t.getCause()) {
            if (t instanceof AgentDomainException domain) {
                return domain;
            }
        }
        return fail("No AgentDomainException in the cause chain of " + thrown);
    }

    private TraceEntry handlerEntry() {
        return trace.entries().stream()
                .filter(e -> e.phase() == Phase.ON_EXCEPTION)
                .findFirst()
                .orElseThrow();
    }

    // -- handler returns, rethrows or throws -----------------------------------

    public record RecoveryEvent(String payload) {}

    @Agent
    @ApplicationScoped
    public static class RecoveryAgent {

        @Inject ExecutionTraceRecorder trace;

        @Trigger
        public void onEvent(@Observes RecoveryEvent event) {
            trace.record(Phase.TRIGGER, "onEvent", event);
        }

        @Action
        public void act(RecoveryEvent event) {
            trace.record(Phase.ACTION, "act", event);
            throw new AgentDomainException("Simulated action failure");
        }

        @OnException
        public void onError(AgentDomainException ex) {
            trace.record(Phase.ON_EXCEPTION, "onError", ex);
            // Returns normally, so the workflow recovers and reaches the @Outcome.
        }

        @Outcome
        public void finish() {
            trace.record(Phase.OUTCOME, "finish");
        }
    }

    /**
     * A handler that returns normally lets the {@code @Outcome} run, giving the full recovery
     * sequence of trigger, action, handler, outcome.
     *
     * <p>Specification section {@code onexception}.
     */
    @Test
    public void handlerNormalReturnAllowsOutcome() {
        trace.reset();
        recoveryEvents.fire(new RecoveryEvent("x"));
        assertEquals(List.of(Phase.TRIGGER, Phase.ACTION, Phase.ON_EXCEPTION, Phase.OUTCOME), trace.phases());
    }

    public record PropagationEvent(String payload) {}

    @Agent
    @ApplicationScoped
    public static class PropagationAgent {

        @Inject ExecutionTraceRecorder trace;

        @Trigger
        public void onEvent(@Observes PropagationEvent event) {
            trace.record(Phase.TRIGGER, "onEvent", event);
        }

        @Action
        public void act(PropagationEvent event) {
            trace.record(Phase.ACTION, "act", event);
            throw new AgentDomainException("Simulated action failure");
        }

        @OnException
        public void onError(AgentDomainException ex) {
            trace.record(Phase.ON_EXCEPTION, "onError", ex);
            throw ex; // Rethrown, so the workflow ends before the @Outcome.
        }
    }

    /**
     * An exception rethrown from a handler ends the workflow before the {@code @Outcome} and
     * propagates to the container.
     *
     * <p>Specification section {@code onexception}.
     */
    @Test
    public void rethrowFromHandlerTerminatesWorkflow() {
        trace.reset();
        Throwable thrown = assertThrows(Throwable.class,
                () -> propagationEvents.fire(new PropagationEvent("x")));
        agentExceptionIn(thrown);
        assertFalse(trace.phases().contains(Phase.OUTCOME));
    }

    /**
     * The handler receives the very exception instance that was thrown.
     *
     * <p>Specification section {@code onexception}.
     */
    @Test
    public void handlerReceivesExactExceptionInstance() {
        trace.reset();
        Throwable thrown = assertThrows(Throwable.class,
                () -> propagationEvents.fire(new PropagationEvent("x")));
        AgentDomainException cause = agentExceptionIn(thrown);
        assertSame(cause, handlerEntry().args()[0]);
    }

    public record RecursiveGuardEvent(String payload) {}

    @Agent
    @ApplicationScoped
    public static class RecursiveGuardAgent {

        @Inject ExecutionTraceRecorder trace;

        @Trigger
        public void onEvent(@Observes RecursiveGuardEvent event) {
            trace.record(Phase.TRIGGER, "onEvent", event);
        }

        @Action
        public void act(RecursiveGuardEvent event) {
            trace.record(Phase.ACTION, "act", event);
            throw new AgentDomainException("Simulated failure");
        }

        @OnException
        public void onError(AgentDomainException ex) {
            trace.record(Phase.ON_EXCEPTION, "onError", ex);
            throw new AgentDomainException("Handler failure — must not recurse");
        }
    }

    /**
     * An exception thrown inside a handler propagates to the container without re-entering the
     * handler.
     *
     * <p>Specification section {@code onexception}.
     */
    @Test
    public void handlerExceptionPropagatesWithoutRecursion() {
        trace.reset();
        Throwable thrown = assertThrows(Throwable.class,
                () -> recursiveGuardEvents.fire(new RecursiveGuardEvent("x")));
        agentExceptionIn(thrown);
        assertEquals(1, trace.phases().stream().filter(p -> p == Phase.ON_EXCEPTION).count());
    }

    // -- handler selection -----------------------------------------------------

    public static class SpecificDomainException extends AgentDomainException {

        private static final long serialVersionUID = 1L;

        public SpecificDomainException(String message) { super(message); }
    }

    public record HierarchyEvent(String payload) {}

    @Agent
    @ApplicationScoped
    public static class HierarchyAgent {

        @Inject ExecutionTraceRecorder trace;

        @Trigger
        public void onEvent(@Observes HierarchyEvent event) {
            trace.record(Phase.TRIGGER, "onEvent", event);
        }

        @Action
        public void act(HierarchyEvent event) {
            trace.record(Phase.ACTION, "act", event);
            throw new SpecificDomainException("Specific failure");
        }

        @OnException
        public void onSpecific(SpecificDomainException ex) {
            trace.record(Phase.ON_EXCEPTION, "onSpecific", ex);
        }

        @OnException
        public void onGeneric(AgentDomainException ex) {
            trace.record(Phase.ON_EXCEPTION, "onGeneric", ex);
        }
    }

    /**
     * The most specific handler is selected when several {@code @OnException} methods exist in
     * the same agent.
     *
     * <p>Specification section {@code onexception}.
     */
    @Test
    public void specificHandlerSelectedOverGeneric() {
        trace.reset();
        hierarchyEvents.fire(new HierarchyEvent("x"));
        assertEquals("onSpecific", handlerEntry().methodName());
    }

    // -- failing phases --------------------------------------------------------

    public record PhaseFailureEvent(String payload) {}

    /** Fails in whichever phase the test selects before firing its event. */
    @Agent
    @ApplicationScoped
    public static class PhaseFailureAgent {

        public enum FailingPhase { NONE, TRIGGER, DECISION, ACTION, OUTCOME }

        @Inject ExecutionTraceRecorder trace;
        private volatile FailingPhase failAt = FailingPhase.NONE;

        public void setFailAt(FailingPhase failAt) {
            this.failAt = failAt;
        }

        @Trigger
        public void onEvent(@Observes PhaseFailureEvent event) {
            trace.record(Phase.TRIGGER, "onEvent", event);
            if (failAt == FailingPhase.TRIGGER)
                throw new AgentDomainException("trigger failure");
        }

        @Decision(order = 1)
        public boolean decide(PhaseFailureEvent event) {
            trace.record(Phase.DECISION, "decide", event);
            if (failAt == FailingPhase.DECISION)
                throw new AgentDomainException("decision failure");
            return true;
        }

        @Action(order = 2)
        public void act(PhaseFailureEvent event) {
            trace.record(Phase.ACTION, "act", event);
            if (failAt == FailingPhase.ACTION)
                throw new AgentDomainException("action failure");
        }

        @Outcome
        public void finish() {
            trace.record(Phase.OUTCOME, "finish");
            if (failAt == FailingPhase.OUTCOME)
                throw new AgentDomainException("outcome failure");
        }

        @OnException
        public void onError(AgentDomainException ex) {
            trace.record(Phase.ON_EXCEPTION, "onError", ex);
        }
    }

    /**
     * The handler is invoked when the exception originates in the {@code @Trigger} phase.
     *
     * <p>Specification section {@code error-handling}.
     */
    @Test
    public void triggerFailureInvokesHandler() {
        trace.reset();
        phaseFailureAgent.setFailAt(PhaseFailureAgent.FailingPhase.TRIGGER);
        phaseFailureEvents.fire(new PhaseFailureEvent("x"));
        assertTrue(trace.phases().containsAll(List.of(Phase.TRIGGER, Phase.ON_EXCEPTION)));
    }

    /**
     * The handler is invoked when the exception originates in the {@code @Decision} phase.
     *
     * <p>Specification section {@code error-handling}.
     */
    @Test
    public void decisionFailureInvokesHandler() {
        trace.reset();
        phaseFailureAgent.setFailAt(PhaseFailureAgent.FailingPhase.DECISION);
        phaseFailureEvents.fire(new PhaseFailureEvent("x"));
        assertTrue(trace.phases().containsAll(List.of(Phase.TRIGGER, Phase.DECISION, Phase.ON_EXCEPTION)));
    }

    /**
     * The handler is invoked when the exception originates in the {@code @Action} phase.
     *
     * <p>Specification section {@code error-handling}.
     */
    @Test
    public void actionFailureInvokesHandler() {
        trace.reset();
        phaseFailureAgent.setFailAt(PhaseFailureAgent.FailingPhase.ACTION);
        phaseFailureEvents.fire(new PhaseFailureEvent("x"));
        assertTrue(trace.phases().containsAll(List.of(Phase.TRIGGER, Phase.ACTION, Phase.ON_EXCEPTION)));
    }

    /**
     * The handler is invoked when the exception originates in the {@code @Outcome} phase.
     *
     * <p>Specification section {@code error-handling}.
     */
    @Test
    public void outcomeFailureInvokesHandler() {
        trace.reset();
        phaseFailureAgent.setFailAt(PhaseFailureAgent.FailingPhase.OUTCOME);
        phaseFailureEvents.fire(new PhaseFailureEvent("x"));
        assertTrue(trace.phases().containsAll(List.of(Phase.TRIGGER, Phase.OUTCOME, Phase.ON_EXCEPTION)));
    }

    // -- no handler ------------------------------------------------------------

    public record NoHandlerEvent(String payload) {}

    @Agent
    @ApplicationScoped
    public static class NoHandlerAgent {

        @Inject ExecutionTraceRecorder trace;

        @Trigger
        public void onEvent(@Observes NoHandlerEvent event) {
            trace.record(Phase.TRIGGER, "onEvent", event);
        }

        @Action
        public void act(NoHandlerEvent event) {
            trace.record(Phase.ACTION, "act", event);
            throw new AgentDomainException("Unhandled failure — no @OnException");
        }

        @Outcome
        public void finish() {
            trace.record(Phase.OUTCOME, "finish");
        }
    }

    /**
     * Without a handler, an exception propagates to the container and the {@code @Outcome} does
     * not run.
     *
     * <p>Specification section {@code error-handling}.
     */
    @Test
    public void uncaughtExceptionPropagatesWithoutHandler() {
        trace.reset();
        Throwable thrown = assertThrows(Throwable.class,
                () -> noHandlerEvents.fire(new NoHandlerEvent("x")));
        agentExceptionIn(thrown);
        assertFalse(trace.phases().contains(Phase.OUTCOME));
    }

    // -- handler parameters ----------------------------------------------------

    public record StatefulHandlerEvent(String payload) {}

    @Agent
    @ApplicationScoped
    public static class StatefulHandlerAgent {

        @Inject ExecutionTraceRecorder trace;

        @Trigger
        public void onEvent(@Observes StatefulHandlerEvent event) {
            trace.record(Phase.TRIGGER, "onEvent", event);
        }

        @Action
        public void act(StatefulHandlerEvent event) {
            trace.record(Phase.ACTION, "act", event);
            throw new AgentDomainException("Simulated action failure");
        }

        @OnException
        public void onError(AgentDomainException ex, StatefulHandlerEvent event) {
            trace.record(Phase.ON_EXCEPTION, "onError", ex, event);
        }

        @Outcome
        public void finish() {
            trace.record(Phase.OUTCOME, "finish");
        }
    }

    /**
     * After the required exception parameter, a handler receives workflow state. The triggering
     * event is resolved as its second parameter.
     *
     * <p>Specification section {@code onexception}.
     */
    @Test
    public void handlerReceivesWorkflowStateAfterException() {
        trace.reset();
        statefulHandlerEvents.fire(new StatefulHandlerEvent("x"));
        assertEquals(List.of(Phase.TRIGGER, Phase.ACTION, Phase.ON_EXCEPTION, Phase.OUTCOME), trace.phases());
        TraceEntry handler = trace.entries().get(2);
        assertInstanceOf(AgentDomainException.class, handler.args()[0]);
        assertEquals("x", assertInstanceOf(StatefulHandlerEvent.class, handler.args()[1]).payload());
    }
}
