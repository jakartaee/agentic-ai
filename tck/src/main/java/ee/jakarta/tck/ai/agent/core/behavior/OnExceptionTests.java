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
import ee.jakarta.tck.ai.agent.core.behavior.agents.errorhandling.StatefulHandlerAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.errorhandling.StatefulHandlerEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.termination.BooleanTerminationAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.termination.BooleanTerminationEvent;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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
        return fail("no AgentDomainException in the cause chain of " + thrown);
    }

    // ── Recovery flow (BHV-001–003) ─────────────────────────────────────────
    @Assertion(id = "AGENTICAI-ONEXCEPTION-BHV-001",
               section = "error-handling",
               strategy = "Handler completing normally allows @Outcome to run, producing the full recovery phase sequence")
    public void handlerNormalReturnAllowsOutcome() {
        trace.reset();
        recoveryEvents.fire(new RecoveryEvent("x"));
        assertEquals(List.of(Phase.TRIGGER, Phase.ACTION, Phase.HANDLE_EXCEPTION, Phase.OUTCOME), trace.phases());
    }
    @Assertion(id = "AGENTICAI-ONEXCEPTION-BHV-002",
               section = "error-handling",
               strategy = "HANDLE_EXCEPTION phase is recorded when @Action throws")
    public void handlerIsInvokedOnActionFailure() {
        trace.reset();
        recoveryEvents.fire(new RecoveryEvent("x"));
        assertTrue(trace.phases().contains(Phase.HANDLE_EXCEPTION));
    }
    @Assertion(id = "AGENTICAI-ONEXCEPTION-BHV-003",
               section = "error-handling",
               strategy = "@Outcome runs as the final phase after a handler returns normally")
    public void workflowProceedsToOutcomeAfterRecovery() {
        trace.reset();
        recoveryEvents.fire(new RecoveryEvent("x"));
        assertEquals(Phase.OUTCOME, trace.phases().get(trace.phases().size() - 1));
    }

    // ── Exception hierarchy (BHV-004) ───────────────────────────────────────
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
    @Assertion(id = "AGENTICAI-ONEXCEPTION-BHV-005",
               section = "error-handling",
               strategy = "An exception thrown inside @OnException propagates to the container without re-entering the handler")
    public void handlerExceptionPropagatesWithoutRecursion() {
        trace.reset();
        Throwable thrown = assertThrows(Throwable.class,
                () -> recursiveGuardEvents.fire(new RecursiveGuardEvent("x")));
        agentExceptionIn(thrown);
        long count = trace.phases().stream()
                .filter(p -> p == Phase.HANDLE_EXCEPTION)
                .count();
        assertEquals(1, count);
    }

    // ── Cross-phase coverage (BHV-006–009) ──────────────────────────────────
    @Assertion(id = "AGENTICAI-ONEXCEPTION-BHV-006",
               section = "error-handling",
               strategy = "@OnException is invoked when an exception originates in the @Trigger phase")
    public void triggerFailureInvokesHandler() {
        trace.reset();
        phaseFailureAgent.setFailAt(PhaseFailureAgent.FailingPhase.TRIGGER);
        phaseFailureEvents.fire(new PhaseFailureEvent("x"));
        assertTrue(trace.phases().containsAll(List.of(Phase.TRIGGER, Phase.HANDLE_EXCEPTION)));
    }
    @Assertion(id = "AGENTICAI-ONEXCEPTION-BHV-007",
               section = "error-handling",
               strategy = "@OnException is invoked when an exception originates in the @Decision phase")
    public void decisionFailureInvokesHandler() {
        trace.reset();
        phaseFailureAgent.setFailAt(PhaseFailureAgent.FailingPhase.DECISION);
        phaseFailureEvents.fire(new PhaseFailureEvent("x"));
        assertTrue(trace.phases().containsAll(List.of(Phase.TRIGGER, Phase.DECISION, Phase.HANDLE_EXCEPTION)));
    }
    @Assertion(id = "AGENTICAI-ONEXCEPTION-BHV-008",
               section = "error-handling",
               strategy = "@OnException is invoked when an exception originates in the @Action phase")
    public void actionFailureInvokesHandler() {
        trace.reset();
        phaseFailureAgent.setFailAt(PhaseFailureAgent.FailingPhase.ACTION);
        phaseFailureEvents.fire(new PhaseFailureEvent("x"));
        assertTrue(trace.phases().containsAll(List.of(Phase.TRIGGER, Phase.ACTION, Phase.HANDLE_EXCEPTION)));
    }
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
    @Assertion(id = "AGENTICAI-ONEXCEPTION-BHV-010",
               section = "error-handling",
               strategy = "An unhandled exception propagates to the container when no @OnException method is present")
    public void uncaughtExceptionPropagatesWithoutHandler() {
        trace.reset();
        Throwable thrown = assertThrows(Throwable.class,
                () -> noHandlerEvents.fire(new NoHandlerEvent("x")));
        agentExceptionIn(thrown);
        assertFalse(trace.phases().contains(Phase.OUTCOME));
    }
    @Assertion(id = "AGENTICAI-ONEXCEPTION-BHV-011",
               section = "error-handling",
               strategy = "The handler receives the exact exception instance that was thrown (referential identity)")
    public void handlerReceivesExactExceptionInstance() {
        trace.reset();
        Throwable thrown = assertThrows(Throwable.class,
                () -> propagationEvents.fire(new PropagationEvent("x")));
        AgentDomainException cause = agentExceptionIn(thrown);
        TraceEntry handlerEntry = trace.entries().stream()
                .filter(e -> e.phase() == Phase.HANDLE_EXCEPTION)
                .findFirst()
                .orElseThrow();
        assertSame(cause, handlerEntry.args()[0]);
    }

    @Assertion(id = "AGENTICAI-ONEXCEPTION-BHV-012",
               section = "error-handling",
               strategy = "After the required exception parameter, an @OnException method receives workflow state: "
                        + "the triggering event is resolved as its second parameter")
    public void handlerReceivesWorkflowStateAfterException() {
        trace.reset();
        StatefulHandlerEvent event = new StatefulHandlerEvent("x");
        statefulHandlerEvents.fire(event);
        assertEquals(List.of(Phase.TRIGGER, Phase.ACTION, Phase.HANDLE_EXCEPTION, Phase.OUTCOME), trace.phases());
        TraceEntry handlerEntry = trace.entries().get(2);
        assertInstanceOf(AgentDomainException.class, handlerEntry.args()[0]);
        assertEquals("x", assertInstanceOf(StatefulHandlerEvent.class, handlerEntry.args()[1]).payload());
    }

    // ── Termination states (TERMINATION-BHV-001–004) ─────────────────────────
    @Assertion(id = "AGENTICAI-TERMINATION-BHV-001",
               section = "agent-lifecycle",
               strategy = "Normal workflow completion ends with @Outcome as the final recorded phase")
    public void normalTerminationCompletesOutcome() {
        trace.reset();
        phaseFailureAgent.setFailAt(PhaseFailureAgent.FailingPhase.NONE);
        phaseFailureEvents.fire(new PhaseFailureEvent("x"));
        assertEquals(Phase.OUTCOME, trace.phases().get(trace.phases().size() - 1));
    }
    @Assertion(id = "AGENTICAI-TERMINATION-BHV-002",
               section = "agent-lifecycle",
               strategy = "@Decision returning false halts execution before @Action and @Outcome")
    public void decisionBasedTerminationSkipsDownstreamPhases() {
        trace.reset();
        booleanEvents.fire(new BooleanTerminationEvent("x"));
        assertEquals(List.of(Phase.TRIGGER, Phase.DECISION), trace.phases());
    }
    @Assertion(id = "AGENTICAI-TERMINATION-BHV-003",
               section = "agent-lifecycle",
               strategy = "An unhandled exception terminates the workflow before @Outcome runs and propagates to the container")
    public void exceptionBasedTerminationAbortsWorkflow() {
        trace.reset();
        Throwable thrown = assertThrows(Throwable.class,
                () -> noHandlerEvents.fire(new NoHandlerEvent("x")));
        agentExceptionIn(thrown);
        assertFalse(trace.phases().contains(Phase.OUTCOME));
    }
    @Assertion(id = "AGENTICAI-TERMINATION-BHV-004",
               section = "agent-lifecycle",
               strategy = "An exception re-thrown from @OnException terminates the workflow without reaching @Outcome and propagates to the container")
    public void handlerFailureTerminatesWorkflow() {
        trace.reset();
        Throwable thrown = assertThrows(Throwable.class,
                () -> propagationEvents.fire(new PropagationEvent("x")));
        agentExceptionIn(thrown);
        assertFalse(trace.phases().contains(Phase.OUTCOME));
    }
}
