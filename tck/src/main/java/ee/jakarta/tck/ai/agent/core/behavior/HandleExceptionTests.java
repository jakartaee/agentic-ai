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
import jakarta.ai.agent.HandleException;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Deployed
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class HandleExceptionTests {

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
    @Assertion(id = "AGENTICAI-HANDLEEXCEPTION-BHV-001-PRECONDITION",
               section = "3.6 Exception Handling",
               strategy = "RecoveryAgent @Trigger is observed by CDI without a compatible implementation")
    public void recoveryAgentTriggerObserved() {
        trace.reset();
        recoveryEvents.fire(new RecoveryEvent("x"));
        assertThat(trace.phases()).containsExactly(Phase.TRIGGER);
    }

    @RequiresNoImplementation
    @Assertion(id = "AGENTICAI-HANDLEEXCEPTION-BHV-002-PRECONDITION",
               section = "3.6 Exception Handling",
               strategy = "PropagationAgent @Trigger is observed by CDI without a compatible implementation")
    public void propagationAgentTriggerObserved() {
        trace.reset();
        propagationEvents.fire(new PropagationEvent("x"));
        assertThat(trace.phases()).containsExactly(Phase.TRIGGER);
    }

    @RequiresNoImplementation
    @Assertion(id = "AGENTICAI-HANDLEEXCEPTION-BHV-004-PRECONDITION",
               section = "3.6 Exception Handling",
               strategy = "HierarchyAgent @Trigger is observed by CDI without a compatible implementation")
    public void hierarchyAgentTriggerObserved() {
        trace.reset();
        hierarchyEvents.fire(new HierarchyEvent("x"));
        assertThat(trace.phases()).containsExactly(Phase.TRIGGER);
    }

    @RequiresNoImplementation
    @Assertion(id = "AGENTICAI-HANDLEEXCEPTION-BHV-005-PRECONDITION",
               section = "3.6 Exception Handling",
               strategy = "RecursiveGuardAgent @Trigger is observed by CDI without a compatible implementation")
    public void recursiveGuardAgentTriggerObserved() {
        trace.reset();
        recursiveGuardEvents.fire(new RecursiveGuardEvent("x"));
        assertThat(trace.phases()).containsExactly(Phase.TRIGGER);
    }

    @RequiresNoImplementation
    @Assertion(id = "AGENTICAI-HANDLEEXCEPTION-BHV-006-PRECONDITION",
               section = "3.6 Exception Handling",
               strategy = "PhaseFailureAgent @Trigger is observed by CDI when no phase is configured to fail")
    public void phaseFailureAgentTriggerObserved() {
        trace.reset();
        phaseFailureAgent.setFailAt(PhaseFailureAgent.FailingPhase.NONE);
        phaseFailureEvents.fire(new PhaseFailureEvent("x"));
        assertThat(trace.phases()).containsExactly(Phase.TRIGGER);
    }

    @RequiresNoImplementation
    @Assertion(id = "AGENTICAI-HANDLEEXCEPTION-BHV-010-PRECONDITION",
               section = "3.6 Exception Handling",
               strategy = "NoHandlerAgent @Trigger is observed by CDI without a compatible implementation")
    public void noHandlerAgentTriggerObserved() {
        trace.reset();
        noHandlerEvents.fire(new NoHandlerEvent("x"));
        assertThat(trace.phases()).containsExactly(Phase.TRIGGER);
    }

    // ── Recovery flow (BHV-001–003) ─────────────────────────────────────────

    @RequiresImplementation
    @Assertion(id = "AGENTICAI-HANDLEEXCEPTION-BHV-001",
               section = "3.6 Exception Handling",
               strategy = "Handler completing normally allows @Outcome to run, producing the full recovery phase sequence")
    public void handlerNormalReturnAllowsOutcome() {
        trace.reset();
        recoveryEvents.fire(new RecoveryEvent("x"));
        assertThat(trace.phases())
                .containsExactly(Phase.TRIGGER, Phase.ACTION, Phase.HANDLE_EXCEPTION, Phase.OUTCOME);
    }

    @RequiresImplementation
    @Assertion(id = "AGENTICAI-HANDLEEXCEPTION-BHV-002",
               section = "3.6 Exception Handling",
               strategy = "HANDLE_EXCEPTION phase is recorded when @Action throws")
    public void handlerIsInvokedOnActionFailure() {
        trace.reset();
        recoveryEvents.fire(new RecoveryEvent("x"));
        assertThat(trace.phases()).contains(Phase.HANDLE_EXCEPTION);
    }

    @RequiresImplementation
    @Assertion(id = "AGENTICAI-HANDLEEXCEPTION-BHV-003",
               section = "3.6 Exception Handling",
               strategy = "@Outcome runs as the final phase after a handler returns normally")
    public void workflowProceedsToOutcomeAfterRecovery() {
        trace.reset();
        recoveryEvents.fire(new RecoveryEvent("x"));
        assertThat(trace.phases()).last().isEqualTo(Phase.OUTCOME);
    }

    // ── Exception hierarchy (BHV-004) ───────────────────────────────────────

    @RequiresImplementation
    @Assertion(id = "AGENTICAI-HANDLEEXCEPTION-BHV-004",
               section = "3.6 Exception Handling",
               strategy = "The most specific handler is selected when multiple @HandleException methods exist in the same agent")
    public void specificHandlerSelectedOverGeneric() {
        trace.reset();
        hierarchyEvents.fire(new HierarchyEvent("x"));
        TraceEntry handlerEntry = trace.entries().stream()
                .filter(e -> e.phase() == Phase.HANDLE_EXCEPTION)
                .findFirst()
                .orElseThrow();
        assertThat(handlerEntry.methodName()).isEqualTo("onSpecific");
    }

    // ── Recursive prevention (BHV-005) ──────────────────────────────────────

    @RequiresImplementation
    @Assertion(id = "AGENTICAI-HANDLEEXCEPTION-BHV-005",
               section = "3.6 Exception Handling",
               strategy = "An exception thrown inside @HandleException propagates immediately without re-entering the handler")
    public void handlerExceptionPropagatesWithoutRecursion() {
        trace.reset();
        // CDI wraps observer exceptions in ObserverException; the cause is the AgentDomainException
        assertThatThrownBy(() -> recursiveGuardEvents.fire(new RecursiveGuardEvent("x")))
                .isInstanceOf(ObserverException.class)
                .hasCauseInstanceOf(AgentDomainException.class);
        long count = trace.phases().stream()
                .filter(p -> p == Phase.HANDLE_EXCEPTION)
                .count();
        assertThat(count).isEqualTo(1);
    }

    // ── Cross-phase coverage (BHV-006–009) ──────────────────────────────────

    @RequiresImplementation
    @Assertion(id = "AGENTICAI-HANDLEEXCEPTION-BHV-006",
               section = "3.6 Exception Handling",
               strategy = "@HandleException is invoked when an exception originates in the @Trigger phase")
    public void triggerFailureInvokesHandler() {
        trace.reset();
        phaseFailureAgent.setFailAt(PhaseFailureAgent.FailingPhase.TRIGGER);
        phaseFailureEvents.fire(new PhaseFailureEvent("x"));
        assertThat(trace.phases()).contains(Phase.TRIGGER, Phase.HANDLE_EXCEPTION);
    }

    @RequiresImplementation
    @Assertion(id = "AGENTICAI-HANDLEEXCEPTION-BHV-007",
               section = "3.6 Exception Handling",
               strategy = "@HandleException is invoked when an exception originates in the @Decision phase")
    public void decisionFailureInvokesHandler() {
        trace.reset();
        phaseFailureAgent.setFailAt(PhaseFailureAgent.FailingPhase.DECISION);
        phaseFailureEvents.fire(new PhaseFailureEvent("x"));
        assertThat(trace.phases()).contains(Phase.TRIGGER, Phase.DECISION, Phase.HANDLE_EXCEPTION);
    }

    @RequiresImplementation
    @Assertion(id = "AGENTICAI-HANDLEEXCEPTION-BHV-008",
               section = "3.6 Exception Handling",
               strategy = "@HandleException is invoked when an exception originates in the @Action phase")
    public void actionFailureInvokesHandler() {
        trace.reset();
        phaseFailureAgent.setFailAt(PhaseFailureAgent.FailingPhase.ACTION);
        phaseFailureEvents.fire(new PhaseFailureEvent("x"));
        assertThat(trace.phases()).contains(Phase.TRIGGER, Phase.ACTION, Phase.HANDLE_EXCEPTION);
    }

    @RequiresImplementation
    @Assertion(id = "AGENTICAI-HANDLEEXCEPTION-BHV-009",
               section = "3.6 Exception Handling",
               strategy = "@HandleException is invoked when an exception originates in the @Outcome phase")
    public void outcomeFailureInvokesHandler() {
        trace.reset();
        phaseFailureAgent.setFailAt(PhaseFailureAgent.FailingPhase.OUTCOME);
        phaseFailureEvents.fire(new PhaseFailureEvent("x"));
        assertThat(trace.phases()).contains(Phase.TRIGGER, Phase.OUTCOME, Phase.HANDLE_EXCEPTION);
    }

    // ── Fallback, parameter identity, method contracts (BHV-010–013) ────────

    @RequiresImplementation
    @Assertion(id = "AGENTICAI-HANDLEEXCEPTION-BHV-010",
               section = "3.6 Exception Handling",
               strategy = "An unhandled exception propagates to the caller when no @HandleException method is present")
    public void uncaughtExceptionPropagatesWithoutHandler() {
        trace.reset();
        // CDI wraps observer exceptions in ObserverException; the cause is the AgentDomainException
        assertThatThrownBy(() -> noHandlerEvents.fire(new NoHandlerEvent("x")))
                .isInstanceOf(ObserverException.class)
                .hasCauseInstanceOf(AgentDomainException.class);
        assertThat(trace.phases()).doesNotContain(Phase.OUTCOME);
    }

    @RequiresImplementation
    @Assertion(id = "AGENTICAI-HANDLEEXCEPTION-BHV-011",
               section = "3.6 Exception Handling",
               strategy = "The handler receives the exact exception instance that was thrown (referential identity)")
    public void handlerReceivesExactExceptionInstance() {
        trace.reset();
        // CDI wraps observer exceptions in ObserverException; unwrap to get the AgentDomainException
        assertThatThrownBy(() -> propagationEvents.fire(new PropagationEvent("x")))
                .isInstanceOf(ObserverException.class)
                .satisfies(thrown -> {
                    AgentDomainException cause = (AgentDomainException) thrown.getCause();
                    TraceEntry handlerEntry = trace.entries().stream()
                            .filter(e -> e.phase() == Phase.HANDLE_EXCEPTION)
                            .findFirst()
                            .orElseThrow();
                    assertThat(handlerEntry.args()[0]).isSameAs(cause);
                });
    }

    @Assertion(id = "AGENTICAI-HANDLEEXCEPTION-BHV-012",
               section = "3.6 Exception Handling",
               strategy = "@HandleException methods must declare a Throwable subtype as the first parameter")
    public void handlerParameterMustBeExceptionType() {
        for (Method m : RecoveryAgent.class.getDeclaredMethods()) {
            if (m.isAnnotationPresent(HandleException.class)) {
                assertThat(m.getParameterCount())
                        .as("@HandleException method '%s' must have at least one parameter", m.getName())
                        .isGreaterThanOrEqualTo(1);
                assertThat(Throwable.class.isAssignableFrom(m.getParameterTypes()[0]))
                        .as("@HandleException method '%s' first parameter must be a Throwable subtype", m.getName())
                        .isTrue();
            }
        }
    }

    @Assertion(id = "AGENTICAI-HANDLEEXCEPTION-BHV-013",
               section = "3.6 Exception Handling",
               strategy = "@HandleException methods must declare void return type")
    public void handlerMustReturnVoid() {
        for (Method m : RecoveryAgent.class.getDeclaredMethods()) {
            if (m.isAnnotationPresent(HandleException.class)) {
                assertThat(m.getReturnType())
                        .as("@HandleException method '%s' must return void", m.getName())
                        .isEqualTo(void.class);
            }
        }
    }

    // ── Termination states (TERMINATION-BHV-001–004) ─────────────────────────

    @RequiresImplementation
    @Assertion(id = "AGENTICAI-TERMINATION-BHV-001",
               section = "3.7 Workflow Termination",
               strategy = "Normal workflow completion ends with @Outcome as the final recorded phase")
    public void normalTerminationCompletesOutcome() {
        trace.reset();
        phaseFailureAgent.setFailAt(PhaseFailureAgent.FailingPhase.NONE);
        phaseFailureEvents.fire(new PhaseFailureEvent("x"));
        assertThat(trace.phases()).last().isEqualTo(Phase.OUTCOME);
    }

    @RequiresImplementation
    @Assertion(id = "AGENTICAI-TERMINATION-BHV-002",
               section = "3.7 Workflow Termination",
               strategy = "@Decision returning false halts execution before @Action and @Outcome")
    public void decisionBasedTerminationSkipsDownstreamPhases() {
        trace.reset();
        booleanEvents.fire(new BooleanTerminationEvent("x"));
        assertThat(trace.phases()).containsExactly(Phase.TRIGGER, Phase.DECISION);
    }

    @RequiresImplementation
    @Assertion(id = "AGENTICAI-TERMINATION-BHV-003",
               section = "3.7 Workflow Termination",
               strategy = "An unhandled exception terminates the workflow before @Outcome runs")
    public void exceptionBasedTerminationAbortsWorkflow() {
        trace.reset();
        // CDI wraps observer exceptions in ObserverException; the cause is the AgentDomainException
        assertThatThrownBy(() -> noHandlerEvents.fire(new NoHandlerEvent("x")))
                .isInstanceOf(ObserverException.class)
                .hasCauseInstanceOf(AgentDomainException.class);
        assertThat(trace.phases()).doesNotContain(Phase.OUTCOME);
    }

    @RequiresImplementation
    @Assertion(id = "AGENTICAI-TERMINATION-BHV-004",
               section = "3.7 Workflow Termination",
               strategy = "An exception re-thrown from @HandleException terminates the workflow immediately without reaching @Outcome")
    public void handlerFailureTerminatesWorkflow() {
        trace.reset();
        // CDI wraps observer exceptions in ObserverException; the cause is the AgentDomainException
        assertThatThrownBy(() -> propagationEvents.fire(new PropagationEvent("x")))
                .isInstanceOf(ObserverException.class)
                .hasCauseInstanceOf(AgentDomainException.class);
        assertThat(trace.phases()).doesNotContain(Phase.OUTCOME);
    }
}
