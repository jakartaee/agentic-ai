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

import ee.jakarta.tck.ai.agent.agents.onexception.AgentDomainException;
import ee.jakarta.tck.ai.agent.agents.onexception.HierarchyAgent;
import ee.jakarta.tck.ai.agent.agents.onexception.HierarchyEvent;
import ee.jakarta.tck.ai.agent.agents.onexception.NoHandlerAgent;
import ee.jakarta.tck.ai.agent.agents.onexception.NoHandlerEvent;
import ee.jakarta.tck.ai.agent.agents.onexception.PhaseFailureAgent;
import ee.jakarta.tck.ai.agent.agents.onexception.PhaseFailureEvent;
import ee.jakarta.tck.ai.agent.agents.onexception.PropagationAgent;
import ee.jakarta.tck.ai.agent.agents.onexception.PropagationEvent;
import ee.jakarta.tck.ai.agent.agents.onexception.RecoveryAgent;
import ee.jakarta.tck.ai.agent.agents.onexception.RecoveryEvent;
import ee.jakarta.tck.ai.agent.agents.onexception.RecursiveGuardAgent;
import ee.jakarta.tck.ai.agent.agents.onexception.RecursiveGuardEvent;
import ee.jakarta.tck.ai.agent.agents.onexception.SpecificDomainException;
import ee.jakarta.tck.ai.agent.agents.onexception.StatefulHandlerAgent;
import ee.jakarta.tck.ai.agent.agents.onexception.StatefulHandlerEvent;
import ee.jakarta.tck.ai.agent.framework.ExecutionTraceRecorder;
import ee.jakarta.tck.ai.agent.framework.ExecutionTraceRecorder.Phase;
import ee.jakarta.tck.ai.agent.framework.ExecutionTraceRecorder.TraceEntry;
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

    /**
     * A handler that returns normally lets the {@code @Outcome} run, giving the full recovery
     * sequence of trigger, action, handler, outcome.
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
     * An exception rethrown from a handler ends the workflow before the {@code @Outcome} and
     * propagates to the container.
     *
     * <p>Specification section {@code error-handling}.
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
     * The most specific handler is selected when several {@code @OnException} methods exist in
     * the same agent.
     *
     * <p>Specification section {@code error-handling}.
     */
    @Test
    public void specificHandlerSelectedOverGeneric() {
        trace.reset();
        hierarchyEvents.fire(new HierarchyEvent("x"));
        assertEquals("onSpecific", handlerEntry().methodName());
    }

    /**
     * An exception thrown inside a handler propagates to the container without re-entering the
     * handler.
     *
     * <p>Specification section {@code error-handling}.
     */
    @Test
    public void handlerExceptionPropagatesWithoutRecursion() {
        trace.reset();
        Throwable thrown = assertThrows(Throwable.class,
                () -> recursiveGuardEvents.fire(new RecursiveGuardEvent("x")));
        agentExceptionIn(thrown);
        assertEquals(1, trace.phases().stream().filter(p -> p == Phase.ON_EXCEPTION).count());
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

    /**
     * The handler receives the very exception instance that was thrown.
     *
     * <p>Specification section {@code error-handling}.
     */
    @Test
    public void handlerReceivesExactExceptionInstance() {
        trace.reset();
        Throwable thrown = assertThrows(Throwable.class,
                () -> propagationEvents.fire(new PropagationEvent("x")));
        AgentDomainException cause = agentExceptionIn(thrown);
        assertSame(cause, handlerEntry().args()[0]);
    }

    /**
     * After the required exception parameter, a handler receives workflow state. The triggering
     * event is resolved as its second parameter.
     *
     * <p>Specification section {@code error-handling}.
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
