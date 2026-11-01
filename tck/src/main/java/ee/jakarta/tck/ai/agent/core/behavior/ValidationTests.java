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

import ee.jakarta.tck.ai.agent.core.behavior.agents.validation.DecisionValidatedAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.validation.DecisionValidationEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.validation.HandlerInput;
import ee.jakarta.tck.ai.agent.core.behavior.agents.validation.HandlerValidatedAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.validation.HandlerValidationEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.validation.InputData;
import ee.jakarta.tck.ai.agent.core.behavior.agents.validation.OutcomeValidatedAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.validation.OutcomeValidationEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.validation.TriggerValidatedAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.validation.TriggerValidationEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.validation.UnhandledValidatedAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.validation.UnhandledValidationEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.validation.ValidatedAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.validation.ValidationEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.errorhandling.AgentDomainException;
import ee.jakarta.tck.ai.agent.framework.junit.anno.Assertion;
import ee.jakarta.tck.ai.agent.framework.junit.anno.Deployed;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder.Phase;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder.TraceEntry;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolationException;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies Jakarta Validation integration in the agent workflow
 * (see the spec, {@code [[jakarta-validation-constraints]]}).
 *
 * <p>Validation is performed by the implementation before parameter
 * injection and phase invocation.</p>
 *
 * <p>Note on reset: in {@code @Deployed} classes {@code @BeforeEach} does not run
 * reliably between methods inside the container, so each test resets the trace
 * inline.</p>
 */
@Deployed
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ValidationTests {

    @Deployment
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "validation.war")
                .addClasses(
                        ValidatedAgent.class, ValidationEvent.class, InputData.class,
                        UnhandledValidatedAgent.class, UnhandledValidationEvent.class,
                        TriggerValidatedAgent.class, TriggerValidationEvent.class,
                        DecisionValidatedAgent.class, DecisionValidationEvent.class,
                        HandlerValidatedAgent.class, HandlerValidationEvent.class, HandlerInput.class,
                        OutcomeValidatedAgent.class, OutcomeValidationEvent.class
                )
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject Event<ValidationEvent>          events;
    @Inject Event<UnhandledValidationEvent> unhandledEvents;
    @Inject Event<TriggerValidationEvent>   triggerEvents;
    @Inject Event<DecisionValidationEvent>  decisionEvents;
    @Inject Event<HandlerValidationEvent>   handlerEvents;
    @Inject Event<OutcomeValidationEvent>   outcomeEvents;
    @Inject ExecutionTraceRecorder          trace;

    // ── Valid input proceeds ──────────────────────────────────────────────────
    @Assertion(id = "AGENTICAI-VALIDATION-BHV-001",
               section = "jakarta-validation-constraints",
               strategy = "Valid input passes validation and the @Action body runs, producing the TRIGGER→ACTION sequence")
    public void validInputProceedsThroughAction() {
        trace.reset();
        events.fire(new ValidationEvent("ok"));
        assertEquals(List.of(Phase.TRIGGER, Phase.ACTION), trace.phases());
    }

    // ── Invalid input: body does not run ─────────────────────────────────────
    @Assertion(id = "AGENTICAI-VALIDATION-BHV-002",
               section = "jakarta-validation-constraints",
               strategy = "Invalid input fails validation before injection, so the @Action body never runs")
    public void invalidInputDoesNotRunActionBody() {
        trace.reset();
        events.fire(new ValidationEvent(null));
        assertFalse(trace.phases().contains(Phase.ACTION));
    }

    // ── Invalid input: routed to @OnException ────────────────────────────
    @Assertion(id = "AGENTICAI-VALIDATION-BHV-003",
               section = "jakarta-validation-constraints",
               strategy = "A ConstraintViolationException from failed validation is routed to a matching @OnException method")
    public void invalidInputRoutedToHandler() {
        trace.reset();
        events.fire(new ValidationEvent(null));

        assertTrue(trace.phases().contains(Phase.HANDLE_EXCEPTION));
        TraceEntry handlerEntry = trace.entries().stream()
                .filter(e -> e.phase() == Phase.HANDLE_EXCEPTION)
                .findFirst()
                .orElseThrow();
        assertInstanceOf(ConstraintViolationException.class, handlerEntry.args()[0]);
    }

    // ── Invalid input with no handler: propagates to the container ───────────
    @Assertion(id = "AGENTICAI-VALIDATION-BHV-004",
               section = "jakarta-validation-constraints",
               strategy = "Without a matching @OnException, the ConstraintViolationException propagates to the container")
    public void invalidInputPropagatesWithoutHandler() {
        trace.reset();
        Throwable thrown = assertThrows(Throwable.class,
                () -> unhandledEvents.fire(new UnhandledValidationEvent(null)));
        Throwable violation = thrown;
        while (violation != null && !(violation instanceof ConstraintViolationException)) {
            violation = violation.getCause();
        }
        assertInstanceOf(ConstraintViolationException.class, violation);
        assertFalse(trace.phases().contains(Phase.ACTION));
    }

    // ── Constraints on other phases ──────────────────────────────────────────
    @Assertion(id = "AGENTICAI-VALIDATION-BHV-005",
               section = "jakarta-validation-constraints",
               strategy = "A constraint on the @Trigger event parameter is validated before the trigger body runs; "
                        + "the ConstraintViolationException is routed to @OnException and no later phase runs")
    public void invalidTriggerEventIsValidatedBeforeTrigger() {
        trace.reset();
        triggerEvents.fire(new TriggerValidationEvent(null));

        assertFalse(trace.phases().contains(Phase.TRIGGER));
        assertFalse(trace.phases().contains(Phase.ACTION));
        TraceEntry handlerEntry = trace.entries().stream()
                .filter(e -> e.phase() == Phase.HANDLE_EXCEPTION)
                .findFirst()
                .orElseThrow();
        assertInstanceOf(ConstraintViolationException.class, handlerEntry.args()[0]);
    }
    @Assertion(id = "AGENTICAI-VALIDATION-BHV-005",
               section = "jakarta-validation-constraints",
               strategy = "A constraint on a @Decision parameter is validated before the decision body runs; "
                        + "the ConstraintViolationException is routed to @OnException and the chain stops")
    public void invalidDecisionParameterIsValidatedBeforeDecision() {
        trace.reset();
        decisionEvents.fire(new DecisionValidationEvent(null));

        assertFalse(trace.phases().contains(Phase.DECISION));
        assertFalse(trace.phases().contains(Phase.ACTION));
        TraceEntry handlerEntry = trace.entries().stream()
                .filter(e -> e.phase() == Phase.HANDLE_EXCEPTION)
                .findFirst()
                .orElseThrow();
        assertInstanceOf(ConstraintViolationException.class, handlerEntry.args()[0]);
    }
    @Assertion(id = "AGENTICAI-VALIDATION-BHV-005",
               section = "jakarta-validation-constraints",
               strategy = "Valid input on the trigger event and the decision parameter runs those phases normally")
    public void validInputOnOtherPhasesProceeds() {
        trace.reset();
        triggerEvents.fire(new TriggerValidationEvent("ok"));
        assertEquals(List.of(Phase.TRIGGER, Phase.ACTION), trace.phases());

        trace.reset();
        decisionEvents.fire(new DecisionValidationEvent("ok"));
        assertEquals(List.of(Phase.TRIGGER, Phase.DECISION, Phase.ACTION), trace.phases());
    }

    @Assertion(id = "AGENTICAI-VALIDATION-BHV-007",
               section = "jakarta-validation-constraints",
               strategy = "A constraint on an @Outcome parameter is validated before the outcome body runs; "
                        + "the ConstraintViolationException is routed to @OnException, and valid input reaches the outcome")
    public void invalidOutcomeParameterIsValidatedBeforeOutcome() {
        trace.reset();
        outcomeEvents.fire(new OutcomeValidationEvent(null));

        assertFalse(trace.phases().contains(Phase.OUTCOME));
        TraceEntry handlerEntry = trace.entries().stream()
                .filter(e -> e.phase() == Phase.HANDLE_EXCEPTION)
                .findFirst()
                .orElseThrow();
        assertInstanceOf(ConstraintViolationException.class, handlerEntry.args()[0]);

        trace.reset();
        outcomeEvents.fire(new OutcomeValidationEvent("ok"));
        assertEquals(List.of(Phase.TRIGGER, Phase.ACTION, Phase.OUTCOME), trace.phases());
    }

    // ── Constraints on @OnException parameters ───────────────────────────────
    @Assertion(id = "AGENTICAI-VALIDATION-BHV-006",
               section = "jakarta-validation-constraints",
               strategy = "A constraint violation on an @OnException parameter propagates to the container instead of "
                        + "re-entering exception handling; the handler body does not run")
    public void invalidHandlerParameterPropagatesWithoutRunningHandler() {
        trace.reset();
        Throwable thrown = assertThrows(Throwable.class,
                () -> handlerEvents.fire(new HandlerValidationEvent(" ")));

        Throwable first = thrown;
        while (first != null
                && !(first instanceof ConstraintViolationException)
                && !(first instanceof AgentDomainException)) {
            first = first.getCause();
        }
        assertInstanceOf(ConstraintViolationException.class, first,
                "the validation exception, not the handled one, must propagate");
        assertEquals(List.of(Phase.TRIGGER, Phase.ACTION), trace.phases());
    }
}