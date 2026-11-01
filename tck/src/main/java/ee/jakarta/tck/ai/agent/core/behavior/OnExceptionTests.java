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

import ee.jakarta.tck.ai.agent.core.behavior.agents.errorhandling.AgentDomainException;
import ee.jakarta.tck.ai.agent.core.behavior.agents.errorhandling.HierarchyAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.errorhandling.HierarchyEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.errorhandling.NoHandlerAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.errorhandling.NoHandlerEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.errorhandling.PhaseFailureAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.errorhandling.PhaseFailureEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.errorhandling.PropagationAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.errorhandling.PropagationEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.errorhandling.RecoveryAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.errorhandling.RecoveryEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.errorhandling.RecursiveGuardAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.errorhandling.RecursiveGuardEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.errorhandling.SpecificDomainException;
import ee.jakarta.tck.ai.agent.core.behavior.agents.termination.BooleanTerminationAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.termination.BooleanTerminationEvent;
import ee.jakarta.tck.ai.agent.framework.junit.anno.Assertion;
import ee.jakarta.tck.ai.agent.framework.junit.anno.Deployed;
import ee.jakarta.tck.ai.agent.framework.junit.anno.RequiresImplementation;
import ee.jakarta.tck.ai.agent.framework.junit.anno.RequiresNoImplementation;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder.Phase;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder.TraceEntry;
import jakarta.ai.agent.OnException;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.ObserverException;
import jakarta.inject.Inject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.TestInstance;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Deployed
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
    @Inject Event<BooleanTerminationEvent> booleanEvents;
    @Inject ExecutionTraceRecorder        trace;
    @Inject PhaseFailureAgent             phaseFailureAgent;

    // ── Smoke checks — CDI fires @Trigger without a compatible implementation ─────────────────

    @RequiresNoImplementation
    @Assertion(id = "AGENTICAI-ONEXCEPTION-BHV-001-PRECONDITION",
               section = "error-handling",
               strategy = "RecoveryAgent @Trigger is observed by CDI without a compatible implementation")
    public void recoveryAgentTriggerObserved() {
        trace.reset();
        recoveryEvents.fire(new RecoveryEvent("x"));
        assertEquals(List.of(Phase.TRIGGER), trace.phases());
    }

    @RequiresNoImplementation
    @Assertion(id = "AGENTICAI-ONEXCEPTION-BHV-002-PRECONDITION",
               section = "error-handling",
               strategy = "PropagationAgent @Trigger is observed by CDI without a compatible implementation")
    public void propagationAgentTriggerObserved() {
        trace.reset();
        propagationEvents.fire(new PropagationEvent("x"));
        assertEquals(List.of(Phase.TRIGGER), trace.phases());
    }

    @RequiresNoImplementation
    @Assertion(id = "AGENTICAI-ONEXCEPTION-BHV-004-PRECONDITION",
               section = "error-handling",
               strategy = "HierarchyAgent @Trigger is observed by CDI without a compatible implementation")
    public void hierarchyAgentTriggerObserved() {
        trace.reset();
        hierarchyEvents.fire(new HierarchyEvent("x"));
        assertEquals(List.of(Phase.TRIGGER), trace.phases());
    }

    @RequiresNoImplementation
    @Assertion(id = "AGENTICAI-ONEXCEPTION-BHV-005-PRECONDITION",
               section = "error-handling",
               strategy = "RecursiveGuardAgent @Trigger is observed by CDI without a compatible implementation")
    public void recursiveGuardAgentTriggerObserved() {
        trace.reset();
        recursiveGuardEvents.fire(new RecursiveGuardEvent("x"));
        assertEquals(List.of(Phase.TRIGGER), trace.phases());
    }

    @RequiresNoImplementation
    @Assertion(id = "AGENTICAI-ONEXCEPTION-BHV-006-PRECONDITION",
               section = "error-handling",
               strategy = "PhaseFailureAgent @Trigger is observed by CDI when no phase is configured to fail")
    public void phaseFailureAgentTriggerObserved() {
        trace.reset();
        phaseFailureAgent.setFailAt(PhaseFailureAgent.FailingPhase.NONE);
        phaseFailureEvents.fire(new PhaseFailureEvent("x"));
        assertEquals(List.of(Phase.TRIGGER), trace.phases());
    }

    @RequiresNoImplementation
    @Assertion(id = "AGENTICAI-ONEXCEPTION-BHV-010-PRECONDITION",
               section = "error-handling",
               strategy = "NoHandlerAgent @Trigger is observed by CDI without a compatible implementation")
    public void noHandlerAgentTriggerObserved() {
        trace.reset();
        noHandlerEvents.fire(new NoHandlerEvent("x"));
        assertEquals(List.of(Phase.TRIGGER), trace.phases());
    }

    // ── Recovery flow (BHV-001–003) ─────────────────────────────────────────

    @RequiresImplementation
    @Assertion(id = "AGENTICAI-ONEXCEPTION-BHV-001",
               section = "error-handling",
               strategy = "Handler completing normally allows @Outcome to run, producing the full recovery phase sequence")
    public void handlerNormalReturnAllowsOutcome() {
        trace.reset();
        recoveryEvents.fire(new RecoveryEvent("x"));
        assertEquals(List.of(Phase.TRIGGER, Phase.ACTION, Phase.HANDLE_EXCEPTION, Phase.OUTCOME), trace.phases());
    }

    @RequiresImplementation
    @Assertion(id = "AGENTICAI-ONEXCEPTION-BHV-002",
               section = "error-handling",
               strategy = "HANDLE_EXCEPTION phase is recorded when @Action throws")
    public void handlerIsInvokedOnActionFailure() {
        trace.reset();
        recoveryEvents.fire(new RecoveryEvent("x"));
        assertTrue(trace.phases().contains(Phase.HANDLE_EXCEPTION));
    }

    @RequiresImplementation
    @Assertion(id = "AGENTICAI-ONEXCEPTION-BHV-003",
               section = "error-handling",
               strategy = "@Outcome runs as the final phase after a handler returns normally")
    public void workflowProceedsToOutcomeAfterRecovery() {
        trace.reset();
        recoveryEvents.fire(new RecoveryEvent("x"));
        assertEquals(Phase.OUTCOME, trace.phases().get(trace.phases().size() - 1));
    }

    // ── Exception hierarchy (BHV-004) ───────────────────────────────────────

    @RequiresImplementation
    @Assertion(id = "AGENTICAI-ONEXCEPTION-BHV-004",
               section = "error-handling",
               strategy = "The most specific handler is selected when multiple @OnException methods exist in the same agent")
    public void specificHandlerSelectedOverGeneric() {
        trace.reset();
        hierarchyEvents.fire(new HierarchyEvent("x"));
        TraceEntry handlerEntry = trace.entries().stream()
                .filter(e -> e.phase() == Phase.HANDLE_EXCEPTION)
                .findFirst()
                .orElseThrow();
        assertEquals("onSpecific", handlerEntry.methodName());
    }

    // ── Recursive prevention (BHV-005) ──────────────────────────────────────

    @RequiresImplementation
    @Assertion(id = "AGENTICAI-ONEXCEPTION-BHV-005",
               section = "error-handling",
               strategy = "An exception thrown inside @OnException propagates immediately without re-entering the handler")
    public void handlerExceptionPropagatesWithoutRecursion() {
        trace.reset();
        // CDI wraps observer exceptions in ObserverException; the cause is the AgentDomainException
        ObserverException thrown = assertThrows(ObserverException.class,
                () -> recursiveGuardEvents.fire(new RecursiveGuardEvent("x")));
        assertInstanceOf(AgentDomainException.class, thrown.getCause());
        long count = trace.phases().stream()
                .filter(p -> p == Phase.HANDLE_EXCEPTION)
                .count();
        assertEquals(1, count);
    }

    // ── Cross-phase coverage (BHV-006–009) ──────────────────────────────────

    @RequiresImplementation
    @Assertion(id = "AGENTICAI-ONEXCEPTION-BHV-006",
               section = "error-handling",
               strategy = "@OnException is invoked when an exception originates in the @Trigger phase")
    public void triggerFailureInvokesHandler() {
        trace.reset();
        phaseFailureAgent.setFailAt(PhaseFailureAgent.FailingPhase.TRIGGER);
        phaseFailureEvents.fire(new PhaseFailureEvent("x"));
        assertTrue(trace.phases().containsAll(List.of(Phase.TRIGGER, Phase.HANDLE_EXCEPTION)));
    }

    @RequiresImplementation
    @Assertion(id = "AGENTICAI-ONEXCEPTION-BHV-007",
               section = "error-handling",
               strategy = "@OnException is invoked when an exception originates in the @Decision phase")
    public void decisionFailureInvokesHandler() {
        trace.reset();
        phaseFailureAgent.setFailAt(PhaseFailureAgent.FailingPhase.DECISION);
        phaseFailureEvents.fire(new PhaseFailureEvent("x"));
        assertTrue(trace.phases().containsAll(List.of(Phase.TRIGGER, Phase.DECISION, Phase.HANDLE_EXCEPTION)));
    }

    @RequiresImplementation
    @Assertion(id = "AGENTICAI-ONEXCEPTION-BHV-008",
               section = "error-handling",
               strategy = "@OnException is invoked when an exception originates in the @Action phase")
    public void actionFailureInvokesHandler() {
        trace.reset();
        phaseFailureAgent.setFailAt(PhaseFailureAgent.FailingPhase.ACTION);
        phaseFailureEvents.fire(new PhaseFailureEvent("x"));
        assertTrue(trace.phases().containsAll(List.of(Phase.TRIGGER, Phase.ACTION, Phase.HANDLE_EXCEPTION)));
    }

    @RequiresImplementation
    @Assertion(id = "AGENTICAI-ONEXCEPTION-BHV-009",
               section = "error-handling",
               strategy = "@OnException is invoked when an exception originates in the @Outcome phase")
    public void outcomeFailureInvokesHandler() {
        trace.reset();
        phaseFailureAgent.setFailAt(PhaseFailureAgent.FailingPhase.OUTCOME);
        phaseFailureEvents.fire(new PhaseFailureEvent("x"));
        assertTrue(trace.phases().containsAll(List.of(Phase.TRIGGER, Phase.OUTCOME, Phase.HANDLE_EXCEPTION)));
    }

    // ── Fallback, parameter identity, method contracts (BHV-010–013) ────────

    @RequiresImplementation
    @Assertion(id = "AGENTICAI-ONEXCEPTION-BHV-010",
               section = "error-handling",
               strategy = "An unhandled exception propagates to the caller when no @OnException method is present")
    public void uncaughtExceptionPropagatesWithoutHandler() {
        trace.reset();
        // CDI wraps observer exceptions in ObserverException; the cause is the AgentDomainException
        ObserverException thrown = assertThrows(ObserverException.class,
                () -> noHandlerEvents.fire(new NoHandlerEvent("x")));
        assertInstanceOf(AgentDomainException.class, thrown.getCause());
        assertFalse(trace.phases().contains(Phase.OUTCOME));
    }

    @RequiresImplementation
    @Assertion(id = "AGENTICAI-ONEXCEPTION-BHV-011",
               section = "error-handling",
               strategy = "The handler receives the exact exception instance that was thrown (referential identity)")
    public void handlerReceivesExactExceptionInstance() {
        trace.reset();
        // CDI wraps observer exceptions in ObserverException; unwrap to get the AgentDomainException
        ObserverException thrown = assertThrows(ObserverException.class,
                () -> propagationEvents.fire(new PropagationEvent("x")));
        AgentDomainException cause = (AgentDomainException) thrown.getCause();
        TraceEntry handlerEntry = trace.entries().stream()
                .filter(e -> e.phase() == Phase.HANDLE_EXCEPTION)
                .findFirst()
                .orElseThrow();
        assertSame(cause, handlerEntry.args()[0]);
    }

    @Assertion(id = "AGENTICAI-ONEXCEPTION-BHV-012",
               section = "error-handling",
               strategy = "@OnException methods must declare a Throwable subtype as the first parameter")
    public void handlerParameterMustBeExceptionType() {
        for (Method m : RecoveryAgent.class.getDeclaredMethods()) {
            if (m.isAnnotationPresent(OnException.class)) {
                assertTrue(m.getParameterCount() >= 1, String.format("@OnException method '%s' must have at least one parameter", m.getName()));
                assertTrue(Throwable.class.isAssignableFrom(m.getParameterTypes()[0]), String.format("@OnException method '%s' first parameter must be a Throwable subtype", m.getName()));
            }
        }
    }

    @Assertion(id = "AGENTICAI-ONEXCEPTION-BHV-013",
               section = "error-handling",
               strategy = "@OnException methods must declare void return type")
    public void handlerMustReturnVoid() {
        for (Method m : RecoveryAgent.class.getDeclaredMethods()) {
            if (m.isAnnotationPresent(OnException.class)) {
                assertEquals(void.class, m.getReturnType(), String.format("@OnException method '%s' must return void", m.getName()));
            }
        }
    }

    // ── Termination states (TERMINATION-BHV-001–004) ─────────────────────────

    @RequiresImplementation
    @Assertion(id = "AGENTICAI-TERMINATION-BHV-001",
               section = "agent-lifecycle",
               strategy = "Normal workflow completion ends with @Outcome as the final recorded phase")
    public void normalTerminationCompletesOutcome() {
        trace.reset();
        phaseFailureAgent.setFailAt(PhaseFailureAgent.FailingPhase.NONE);
        phaseFailureEvents.fire(new PhaseFailureEvent("x"));
        assertEquals(Phase.OUTCOME, trace.phases().get(trace.phases().size() - 1));
    }

    @RequiresImplementation
    @Assertion(id = "AGENTICAI-TERMINATION-BHV-002",
               section = "agent-lifecycle",
               strategy = "@Decision returning false halts execution before @Action and @Outcome")
    public void decisionBasedTerminationSkipsDownstreamPhases() {
        trace.reset();
        booleanEvents.fire(new BooleanTerminationEvent("x"));
        assertEquals(List.of(Phase.TRIGGER, Phase.DECISION), trace.phases());
    }

    @RequiresImplementation
    @Assertion(id = "AGENTICAI-TERMINATION-BHV-003",
               section = "agent-lifecycle",
               strategy = "An unhandled exception terminates the workflow before @Outcome runs")
    public void exceptionBasedTerminationAbortsWorkflow() {
        trace.reset();
        // CDI wraps observer exceptions in ObserverException; the cause is the AgentDomainException
        ObserverException thrown = assertThrows(ObserverException.class,
                () -> noHandlerEvents.fire(new NoHandlerEvent("x")));
        assertInstanceOf(AgentDomainException.class, thrown.getCause());
        assertFalse(trace.phases().contains(Phase.OUTCOME));
    }

    @RequiresImplementation
    @Assertion(id = "AGENTICAI-TERMINATION-BHV-004",
               section = "agent-lifecycle",
               strategy = "An exception re-thrown from @OnException terminates the workflow immediately without reaching @Outcome")
    public void handlerFailureTerminatesWorkflow() {
        trace.reset();
        // CDI wraps observer exceptions in ObserverException; the cause is the AgentDomainException
        ObserverException thrown = assertThrows(ObserverException.class,
                () -> propagationEvents.fire(new PropagationEvent("x")));
        assertInstanceOf(AgentDomainException.class, thrown.getCause());
        assertFalse(trace.phases().contains(Phase.OUTCOME));
    }
}
