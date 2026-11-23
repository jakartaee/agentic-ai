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
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies Jakarta Validation integration in the agent workflow
 * (see the spec, {@code [[jakarta-validation-constraints]]}).
 *
 * <p>Validation is performed by the implementation before parameter
 * injection and phase invocation.</p>
 *
 * <p>Note on reset: in Arquillian test classes {@code @BeforeEach} does not run
 * reliably between methods inside the container, so each test resets the trace
 * inline.</p>
 */
@ExtendWith(ArquillianExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ValidationTests {

    @Deployment
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "validation.war")
                .addClasses(
                        ValidatedAgent.class,
                        ValidationEvent.class,
                        InputData.class,
                        UnhandledValidatedAgent.class,
                        UnhandledValidationEvent.class,
                        TriggerValidatedAgent.class,
                        TriggerValidationEvent.class,
                        DecisionValidatedAgent.class,
                        DecisionValidationEvent.class,
                        HandlerValidatedAgent.class,
                        HandlerValidationEvent.class,
                        HandlerInput.class,
                        AgentDomainException.class,
                        OutcomeValidatedAgent.class,
                        OutcomeValidationEvent.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject
    Event<ValidationEvent> events;

    @Inject
    Event<UnhandledValidationEvent> unhandledEvents;

    @Inject
    Event<TriggerValidationEvent> triggerEvents;

    @Inject
    Event<DecisionValidationEvent> decisionEvents;

    @Inject
    Event<HandlerValidationEvent> handlerEvents;

    @Inject
    Event<OutcomeValidationEvent> outcomeEvents;

    @Inject
    ExecutionTraceRecorder trace;

    /**
     * Produced by one phase and received by a later phase that declares it {@code @Valid}. A
     * {@code null} {@code value} violates its {@code @NotNull} constraint.
     */
    public record InputData(@NotNull String value) {}

    // -- valid input proceeds --------------------------------------------------

    /**
     * A non-{@code null} payload produces valid {@link InputData}, a {@code null} payload input
     * that fails validation.
     */
    public record ValidationEvent(String payload) {}

    /**
     * The {@code @Trigger} always runs and produces an {@link InputData} carrying the event
     * payload. The {@code @Action} receives it with {@code @Valid}, so the input is validated
     * <em>before</em> the action body runs, and a violation is routed to the
     * {@code @OnException} method instead.
     */
    @Agent
    @ApplicationScoped
    public static class ValidatedAgent {

        @Inject
        ExecutionTraceRecorder trace;

        @Trigger
        public InputData onEvent(@Observes ValidationEvent event) {
            trace.record(Phase.TRIGGER, "onEvent", event);
            return new InputData(event.payload());
        }

        @Action
        public void act(@Valid InputData data) {
            trace.record(Phase.ACTION, "act", data);
        }

        @OnException
        public void onInvalid(ConstraintViolationException ex) {
            trace.record(Phase.ON_EXCEPTION, "onInvalid", ex);
        }
    }

    /**
     * Valid input passes validation and the {@code @Action} body runs, producing the
     * TRIGGER→ACTION sequence.
     *
     * <p>Specification section {@code jakarta-validation-constraints}.
     */
    @Test
    public void validInputProceedsThroughAction() {
        trace.reset();
        events.fire(new ValidationEvent("ok"));

        assertEquals(List.of(Phase.TRIGGER, Phase.ACTION), trace.phases());
    }

    // -- invalid input skips the phase body ------------------------------------

    /**
     * Invalid input fails validation before injection, so the {@code @Action} body never runs.
     *
     * <p>Specification section {@code jakarta-validation-constraints}.
     */
    @Test
    public void invalidInputDoesNotRunActionBody() {
        trace.reset();
        events.fire(new ValidationEvent(null));

        assertFalse(trace.phases().contains(Phase.ACTION));
    }

    // -- invalid input routed to @OnException ----------------------------------

    /**
     * A ConstraintViolationException from failed validation is routed to a matching {@code
     * @OnException} method.
     *
     * <p>Specification section {@code jakarta-validation-constraints}.
     */
    @Test
    public void invalidInputRoutedToHandler() {
        trace.reset();
        events.fire(new ValidationEvent(null));

        assertTrue(trace.phases().contains(Phase.ON_EXCEPTION));
        TraceEntry handlerEntry = trace.entries().stream()
                .filter(e -> e.phase() == Phase.ON_EXCEPTION)
                .findFirst()
                .orElseThrow();
        assertInstanceOf(ConstraintViolationException.class, handlerEntry.args()[0]);
    }

    // -- invalid input with no handler -----------------------------------------

    public record UnhandledValidationEvent(String payload) {}

    /**
     * Like {@link ValidatedAgent} but with no {@code @OnException} method, so a validation
     * failure has no matching handler and must propagate to the container.
     */
    @Agent
    @ApplicationScoped
    public static class UnhandledValidatedAgent {

        @Inject
        ExecutionTraceRecorder trace;

        @Trigger
        public InputData onEvent(@Observes UnhandledValidationEvent event) {
            trace.record(Phase.TRIGGER, "onEvent", event);
            return new InputData(event.payload());
        }

        @Action
        public void act(@Valid InputData data) {
            trace.record(Phase.ACTION, "act", data);
        }
    }

    /**
     * Without a matching {@code @OnException}, the ConstraintViolationException propagates to the
     * container.
     *
     * <p>Specification section {@code jakarta-validation-constraints}.
     */
    @Test
    public void invalidInputPropagatesWithoutHandler() {
        trace.reset();
        Throwable thrown =
                assertThrows(Throwable.class, () -> unhandledEvents.fire(new UnhandledValidationEvent(null)));

        Throwable violation = thrown;
        while (violation != null && !(violation instanceof ConstraintViolationException)) {
            violation = violation.getCause();
        }
        assertInstanceOf(ConstraintViolationException.class, violation);
        assertFalse(trace.phases().contains(Phase.ACTION));
    }

    // -- constraints on other phases -------------------------------------------

    /** Carries a constraint of its own, so validation applies to the {@code @Trigger} parameter itself. */
    public record TriggerValidationEvent(@NotNull String payload) {}

    @Agent
    @ApplicationScoped
    public static class TriggerValidatedAgent {

        @Inject
        ExecutionTraceRecorder trace;

        @Trigger
        public void onEvent(@Observes @Valid TriggerValidationEvent event) {
            trace.record(Phase.TRIGGER, "onEvent", event);
        }

        @Action
        public void act(TriggerValidationEvent event) {
            trace.record(Phase.ACTION, "act", event);
        }

        @OnException
        public void onInvalid(ConstraintViolationException ex) {
            trace.record(Phase.ON_EXCEPTION, "onInvalid", ex);
        }
    }

    /**
     * A constraint on the {@code @Trigger} event parameter is validated before the trigger body
     * runs; the ConstraintViolationException is routed to {@code @OnException} and no later phase
     * runs.
     *
     * <p>Specification section {@code jakarta-validation-constraints}.
     */
    @Test
    public void invalidTriggerEventIsValidatedBeforeTrigger() {
        trace.reset();
        triggerEvents.fire(new TriggerValidationEvent(null));

        assertFalse(trace.phases().contains(Phase.TRIGGER));
        assertFalse(trace.phases().contains(Phase.ACTION));
        TraceEntry handlerEntry = trace.entries().stream()
                .filter(e -> e.phase() == Phase.ON_EXCEPTION)
                .findFirst()
                .orElseThrow();
        assertInstanceOf(ConstraintViolationException.class, handlerEntry.args()[0]);
    }

    public record DecisionValidationEvent(String payload) {}

    @Agent
    @ApplicationScoped
    public static class DecisionValidatedAgent {

        @Inject
        ExecutionTraceRecorder trace;

        @Trigger
        public InputData onEvent(@Observes DecisionValidationEvent event) {
            trace.record(Phase.TRIGGER, "onEvent", event);
            return new InputData(event.payload());
        }

        @Decision(order = 1)
        public boolean decide(@Valid InputData data) {
            trace.record(Phase.DECISION, "decide", data);
            return true;
        }

        @Action(order = 2)
        public void act(InputData data) {
            trace.record(Phase.ACTION, "act", data);
        }

        @OnException
        public void onInvalid(ConstraintViolationException ex) {
            trace.record(Phase.ON_EXCEPTION, "onInvalid", ex);
        }
    }

    /**
     * A constraint on a {@code @Decision} parameter is validated before the decision body runs;
     * the ConstraintViolationException is routed to {@code @OnException} and the chain stops.
     *
     * <p>Specification section {@code jakarta-validation-constraints}.
     */
    @Test
    public void invalidDecisionParameterIsValidatedBeforeDecision() {
        trace.reset();
        decisionEvents.fire(new DecisionValidationEvent(null));

        assertFalse(trace.phases().contains(Phase.DECISION));
        assertFalse(trace.phases().contains(Phase.ACTION));
        TraceEntry handlerEntry = trace.entries().stream()
                .filter(e -> e.phase() == Phase.ON_EXCEPTION)
                .findFirst()
                .orElseThrow();
        assertInstanceOf(ConstraintViolationException.class, handlerEntry.args()[0]);
    }

    /**
     * Valid input on the trigger event and the decision parameter runs those phases normally.
     *
     * <p>Specification section {@code jakarta-validation-constraints}.
     */
    @Test
    public void validInputOnOtherPhasesProceeds() {
        trace.reset();
        triggerEvents.fire(new TriggerValidationEvent("ok"));
        assertEquals(List.of(Phase.TRIGGER, Phase.ACTION), trace.phases());

        trace.reset();
        decisionEvents.fire(new DecisionValidationEvent("ok"));
        assertEquals(List.of(Phase.TRIGGER, Phase.DECISION, Phase.ACTION), trace.phases());
    }

    public record OutcomeValidationEvent(String payload) {}

    /**
     * The {@code @Action} returns an {@link InputData} built from the event payload (return
     * values are not validated) and the {@code @Outcome} receives it with {@code @Valid}, so a
     * {@code null} payload fails validation at the outcome.
     */
    @Agent
    @ApplicationScoped
    public static class OutcomeValidatedAgent {

        @Inject
        ExecutionTraceRecorder trace;

        @Trigger
        public void onEvent(@Observes OutcomeValidationEvent event) {
            trace.record(Phase.TRIGGER, "onEvent", event);
        }

        @Action
        public InputData act(OutcomeValidationEvent event) {
            trace.record(Phase.ACTION, "act", event);
            return new InputData(event.payload());
        }

        @Outcome
        public void finish(@Valid InputData data) {
            trace.record(Phase.OUTCOME, "finish", data);
        }

        @OnException
        public void onInvalid(ConstraintViolationException ex) {
            trace.record(Phase.ON_EXCEPTION, "onInvalid", ex);
        }
    }

    /**
     * A constraint on an {@code @Outcome} parameter is validated before the outcome body runs; the
     * ConstraintViolationException is routed to {@code @OnException}, and valid input reaches the
     * outcome.
     *
     * <p>Specification section {@code jakarta-validation-constraints}.
     */
    @Test
    public void invalidOutcomeParameterIsValidatedBeforeOutcome() {
        trace.reset();
        outcomeEvents.fire(new OutcomeValidationEvent(null));

        assertFalse(trace.phases().contains(Phase.OUTCOME));
        TraceEntry handlerEntry = trace.entries().stream()
                .filter(e -> e.phase() == Phase.ON_EXCEPTION)
                .findFirst()
                .orElseThrow();
        assertInstanceOf(ConstraintViolationException.class, handlerEntry.args()[0]);

        trace.reset();
        outcomeEvents.fire(new OutcomeValidationEvent("ok"));
        assertEquals(List.of(Phase.TRIGGER, Phase.ACTION, Phase.OUTCOME), trace.phases());
    }

    // -- constraints on @OnException parameters --------------------------------

    public static class AgentDomainException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public AgentDomainException(String message) {
            super(message);
        }
    }

    /**
     * Returned by a {@code @Trigger} and consumed only by an {@code @OnException} method with
     * {@code @Valid}. Return values are not validated, so a blank {@code note} reaches the
     * handler's parameter validation untouched.
     */
    public record HandlerInput(@NotBlank String note) {}

    public record HandlerValidationEvent(String note) {}

    /**
     * The {@code @Trigger} returns a {@link HandlerInput} built from the event, the
     * {@code @Action} always fails, and the {@code @OnException} method would receive the input
     * with {@code @Valid}. When the input is invalid, validation of the handler's parameters
     * fails and the {@code ConstraintViolationException} must propagate to the container rather
     * than re-enter exception handling; the handler body must not run.
     */
    @Agent
    @ApplicationScoped
    public static class HandlerValidatedAgent {

        @Inject
        ExecutionTraceRecorder trace;

        @Trigger
        public HandlerInput onEvent(@Observes HandlerValidationEvent event) {
            trace.record(Phase.TRIGGER, "onEvent", event);
            return new HandlerInput(event.note());
        }

        @Action
        public void act(HandlerValidationEvent event) {
            trace.record(Phase.ACTION, "act", event);
            throw new AgentDomainException("action failed");
        }

        @OnException
        public void handle(AgentDomainException ex, @Valid HandlerInput input) {
            trace.record(Phase.ON_EXCEPTION, "handle", ex, input);
        }
    }

    /**
     * A constraint violation on an {@code @OnException} parameter propagates to the container
     * instead of re-entering exception handling; the handler body does not run.
     *
     * <p>Specification section {@code jakarta-validation-constraints}.
     */
    @Test
    public void invalidHandlerParameterPropagatesWithoutRunningHandler() {
        trace.reset();
        Throwable thrown = assertThrows(Throwable.class, () -> handlerEvents.fire(new HandlerValidationEvent(" ")));

        Throwable first = thrown;
        while (first != null
                && !(first instanceof ConstraintViolationException)
                && !(first instanceof AgentDomainException)) {
            first = first.getCause();
        }
        assertInstanceOf(
                ConstraintViolationException.class,
                first,
                "The validation exception, not the handled one, must propagate");
        assertEquals(List.of(Phase.TRIGGER, Phase.ACTION), trace.phases());
    }
}
