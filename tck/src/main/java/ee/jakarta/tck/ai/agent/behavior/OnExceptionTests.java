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

import ee.jakarta.tck.ai.agent.behavior.agents.errorhandling.AgentDomainException;
import ee.jakarta.tck.ai.agent.behavior.agents.errorhandling.HierarchyAgent;
import ee.jakarta.tck.ai.agent.behavior.agents.errorhandling.HierarchyEvent;
import ee.jakarta.tck.ai.agent.behavior.agents.errorhandling.NoHandlerAgent;
import ee.jakarta.tck.ai.agent.behavior.agents.errorhandling.NoHandlerEvent;
import ee.jakarta.tck.ai.agent.behavior.agents.errorhandling.PhaseFailureAgent;
import ee.jakarta.tck.ai.agent.behavior.agents.errorhandling.PhaseFailureEvent;
import ee.jakarta.tck.ai.agent.behavior.agents.errorhandling.PropagationAgent;
import ee.jakarta.tck.ai.agent.behavior.agents.errorhandling.PropagationEvent;
import ee.jakarta.tck.ai.agent.behavior.agents.errorhandling.RecoveryAgent;
import ee.jakarta.tck.ai.agent.behavior.agents.errorhandling.RecoveryEvent;
import ee.jakarta.tck.ai.agent.behavior.agents.errorhandling.RecursiveGuardAgent;
import ee.jakarta.tck.ai.agent.behavior.agents.errorhandling.RecursiveGuardEvent;
import ee.jakarta.tck.ai.agent.behavior.agents.errorhandling.SpecificDomainException;
import ee.jakarta.tck.ai.agent.behavior.agents.errorhandling.StatefulHandlerAgent;
import ee.jakarta.tck.ai.agent.behavior.agents.errorhandling.StatefulHandlerEvent;
import ee.jakarta.tck.ai.agent.behavior.agents.termination.BooleanTerminationAgent;
import ee.jakarta.tck.ai.agent.behavior.agents.termination.BooleanTerminationEvent;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(ArquillianExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class OnExceptionTests {

    @Deployment
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "handleexception.war")
                .addClasses(
                        AgentDomainException.class,   SpecificDomainException.class,
                        RecoveryAgent.class,           RecoveryEvent.class,
                        PropagationAgent.class,        PropagationEvent.class,
                        HierarchyAgent.class,          HierarchyEvent.class,
                        RecursiveGuardAgent.class,     RecursiveGuardEvent.class,
                        PhaseFailureAgent.class,       PhaseFailureEvent.class,
                        NoHandlerAgent.class,          NoHandlerEvent.class,
                        StatefulHandlerAgent.class,    StatefulHandlerEvent.class,
                        BooleanTerminationAgent.class, BooleanTerminationEvent.class
                )
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject Event<RecoveryEvent>          recoveryEvents;
    @Inject Event<PropagationEvent>       propagationEvents;
    @Inject Event<HierarchyEvent>         hierarchyEvents;
    @Inject Event<RecursiveGuardEvent>    recursiveGuardEvents;
    @Inject Event<PhaseFailureEvent>      phaseFailureEvents;
    @Inject Event<NoHandlerEvent>         noHandlerEvents;
    @Inject Event<StatefulHandlerEvent>   statefulHandlerEvents;
    @Inject Event<BooleanTerminationEvent> booleanEvents;
    @Inject ExecutionTraceRecorder        trace;
    @Inject PhaseFailureAgent             phaseFailureAgent;

    // The container may rethrow the agent's exception as is or wrapped; the spec only says it propagates.
    private static AgentDomainException agentExceptionIn(Throwable thrown) {
        for (Throwable t = thrown; t != null; t = t.getCause()) {
            if (t instanceof AgentDomainException domain) {
                return domain;
            }
        }
        return fail("No AgentDomainException in the cause chain of " + thrown);
    }

    // ── Recovery flow (BHV-001–003) ─────────────────────────────────────────
    /**
     * Handler completing normally allows {@code @Outcome} to run, producing the full recovery
     * phase sequence.
     *
     * <p>Specification section {@code error-handling}.
     */
    @Test
    public void handlerNormalReturnAllowsOutcome() {
        trace.reset();
        recoveryEvents.fire(new RecoveryEvent("x"));
        assertEquals(List.of(Phase.TRIGGER, Phase.ACTION, Phase.ON_EXCEPTION, Phase.OUTCOME), trace.phases());
    }
    /**
     * ON_EXCEPTION phase is recorded when {@code @Action} throws.
     *
     * <p>Specification section {@code error-handling}.
     */
    @Test
    public void handlerIsInvokedOnActionFailure() {
        trace.reset();
        recoveryEvents.fire(new RecoveryEvent("x"));
        assertTrue(trace.phases().contains(Phase.ON_EXCEPTION));
    }
    /**
     * {@code @Outcome} runs as the final phase after a handler returns normally.
     *
     * <p>Specification section {@code error-handling}.
     */
    @Test
    public void workflowProceedsToOutcomeAfterRecovery() {
        trace.reset();
        recoveryEvents.fire(new RecoveryEvent("x"));
        assertEquals(Phase.OUTCOME, trace.phases().get(trace.phases().size() - 1));
    }

    // ── Exception hierarchy (BHV-004) ───────────────────────────────────────
    /**
     * The most specific handler is selected when multiple {@code @OnException} methods exist in
     * the same agent.
     *
     * <p>Specification section {@code error-handling}.
     */
    @Test
    public void specificHandlerSelectedOverGeneric() {
        trace.reset();
        hierarchyEvents.fire(new HierarchyEvent("x"));
        TraceEntry handlerEntry = trace.entries().stream()
                .filter(e -> e.phase() == Phase.ON_EXCEPTION)
                .findFirst()
                .orElseThrow();
        assertEquals("onSpecific", handlerEntry.methodName());
    }

    // ── Recursive prevention (BHV-005) ──────────────────────────────────────
    /**
     * An exception thrown inside {@code @OnException} propagates to the container without re-
     * entering the handler.
     *
     * <p>Specification section {@code error-handling}.
     */
    @Test
    public void handlerExceptionPropagatesWithoutRecursion() {
        trace.reset();
        Throwable thrown = assertThrows(Throwable.class,
                () -> recursiveGuardEvents.fire(new RecursiveGuardEvent("x")));
        agentExceptionIn(thrown);
        long count = trace.phases().stream()
                .filter(p -> p == Phase.ON_EXCEPTION)
                .count();
        assertEquals(1, count);
    }

    // ── Cross-phase coverage (BHV-006–009) ──────────────────────────────────
    /**
     * {@code @OnException} is invoked when an exception originates in the {@code @Trigger} phase.
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
     * {@code @OnException} is invoked when an exception originates in the {@code @Decision} phase.
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
     * {@code @OnException} is invoked when an exception originates in the {@code @Action} phase.
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
     * {@code @OnException} is invoked when an exception originates in the {@code @Outcome} phase.
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

    // ── Fallback, parameter identity, method contracts (BHV-010–013) ────────
    /**
     * An unhandled exception propagates to the container when no {@code @OnException} method is
     * present.
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
    /**
     * The handler receives the exact exception instance that was thrown (referential identity).
     *
     * <p>Specification section {@code error-handling}.
     */
    @Test
    public void handlerReceivesExactExceptionInstance() {
        trace.reset();
        Throwable thrown = assertThrows(Throwable.class,
                () -> propagationEvents.fire(new PropagationEvent("x")));
        AgentDomainException cause = agentExceptionIn(thrown);
        TraceEntry handlerEntry = trace.entries().stream()
                .filter(e -> e.phase() == Phase.ON_EXCEPTION)
                .findFirst()
                .orElseThrow();
        assertSame(cause, handlerEntry.args()[0]);
    }

    /**
     * After the required exception parameter, an {@code @OnException} method receives workflow
     * state. The triggering event is resolved as its second parameter.
     *
     * <p>Specification section {@code error-handling}.
     */
    @Test
    public void handlerReceivesWorkflowStateAfterException() {
        trace.reset();
        StatefulHandlerEvent event = new StatefulHandlerEvent("x");
        statefulHandlerEvents.fire(event);
        assertEquals(List.of(Phase.TRIGGER, Phase.ACTION, Phase.ON_EXCEPTION, Phase.OUTCOME), trace.phases());
        TraceEntry handlerEntry = trace.entries().get(2);
        assertInstanceOf(AgentDomainException.class, handlerEntry.args()[0]);
        assertEquals("x", assertInstanceOf(StatefulHandlerEvent.class, handlerEntry.args()[1]).payload());
    }

    // ── Termination states (TERMINATION-BHV-001–004) ─────────────────────────
    /**
     * Normal workflow completion ends with {@code @Outcome} as the final recorded phase.
     *
     * <p>Specification section {@code agent-lifecycle}.
     */
    @Test
    public void normalTerminationCompletesOutcome() {
        trace.reset();
        phaseFailureAgent.setFailAt(PhaseFailureAgent.FailingPhase.NONE);
        phaseFailureEvents.fire(new PhaseFailureEvent("x"));
        assertEquals(Phase.OUTCOME, trace.phases().get(trace.phases().size() - 1));
    }
    /**
     * {@code @Decision} returning false halts execution before {@code @Action} and {@code
     * @Outcome}.
     *
     * <p>Specification section {@code agent-lifecycle}.
     */
    @Test
    public void decisionBasedTerminationSkipsDownstreamPhases() {
        trace.reset();
        booleanEvents.fire(new BooleanTerminationEvent("x"));
        assertEquals(List.of(Phase.TRIGGER, Phase.DECISION), trace.phases());
    }
    /**
     * An unhandled exception terminates the workflow before {@code @Outcome} runs and propagates
     * to the container.
     *
     * <p>Specification section {@code agent-lifecycle}.
     */
    @Test
    public void exceptionBasedTerminationAbortsWorkflow() {
        trace.reset();
        Throwable thrown = assertThrows(Throwable.class,
                () -> noHandlerEvents.fire(new NoHandlerEvent("x")));
        agentExceptionIn(thrown);
        assertFalse(trace.phases().contains(Phase.OUTCOME));
    }
    /**
     * An exception re-thrown from {@code @OnException} terminates the workflow without reaching
     * {@code @Outcome} and propagates to the container.
     *
     * <p>Specification section {@code agent-lifecycle}.
     */
    @Test
    public void handlerFailureTerminatesWorkflow() {
        trace.reset();
        Throwable thrown = assertThrows(Throwable.class,
                () -> propagationEvents.fire(new PropagationEvent("x")));
        agentExceptionIn(thrown);
        assertFalse(trace.phases().contains(Phase.OUTCOME));
    }
}
