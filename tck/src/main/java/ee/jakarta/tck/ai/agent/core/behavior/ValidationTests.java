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

import ee.jakarta.tck.ai.agent.core.behavior.agents.validation.InputData;
import ee.jakarta.tck.ai.agent.core.behavior.agents.validation.UnhandledValidatedAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.validation.UnhandledValidationEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.validation.ValidatedAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.validation.ValidationEvent;
import ee.jakarta.tck.ai.agent.framework.junit.anno.Assertion;
import ee.jakarta.tck.ai.agent.framework.junit.anno.Deployed;
import ee.jakarta.tck.ai.agent.framework.junit.anno.RequiresImplementation;
import ee.jakarta.tck.ai.agent.framework.junit.anno.RequiresNoImplementation;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies Jakarta Validation integration in the agent workflow
 * (see the spec, {@code [[jakarta-validation-constraints]]}).
 *
 * <p>Validation is performed by a compatible implementation before parameter
 * injection and phase invocation, so the behavioral assertions are
 * {@link RequiresImplementation}. Without an implementation, plain CDI dispatches
 * only the {@code @Trigger}; the {@link RequiresNoImplementation} baseline covers
 * that case.</p>
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
                        UnhandledValidatedAgent.class, UnhandledValidationEvent.class
                )
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject Event<ValidationEvent>          events;
    @Inject Event<UnhandledValidationEvent> unhandledEvents;
    @Inject ExecutionTraceRecorder          trace;

    // ── Baseline — CDI fires @Trigger without a compatible implementation ─────

    @RequiresNoImplementation
    @Assertion(id = "AGENTICAI-VALIDATION-BHV-001-PRECONDITION",
               section = "jakarta-validation-constraints",
               strategy = "ValidatedAgent @Trigger is observed by CDI without a compatible implementation; "
                        + "no validation is performed in plain CDI")
    public void triggerObservedWithoutImplementation() {
        trace.reset();
        events.fire(new ValidationEvent("ok"));
        assertThat(trace.phases()).containsExactly(Phase.TRIGGER);
    }

    // ── Valid input proceeds ──────────────────────────────────────────────────

    @RequiresImplementation
    @Assertion(id = "AGENTICAI-VALIDATION-BHV-001",
               section = "jakarta-validation-constraints",
               strategy = "Valid input passes validation and the @Action body runs, producing the TRIGGER→ACTION sequence")
    public void validInputProceedsThroughAction() {
        trace.reset();
        events.fire(new ValidationEvent("ok"));
        assertThat(trace.phases()).containsExactly(Phase.TRIGGER, Phase.ACTION);
    }

    // ── Invalid input: body does not run ─────────────────────────────────────

    @RequiresImplementation
    @Assertion(id = "AGENTICAI-VALIDATION-BHV-002",
               section = "jakarta-validation-constraints",
               strategy = "Invalid input fails validation before injection, so the @Action body never runs")
    public void invalidInputDoesNotRunActionBody() {
        trace.reset();
        events.fire(new ValidationEvent(null));
        assertThat(trace.phases()).doesNotContain(Phase.ACTION);
    }

    // ── Invalid input: routed to @HandleException ────────────────────────────

    @RequiresImplementation
    @Assertion(id = "AGENTICAI-VALIDATION-BHV-003",
               section = "jakarta-validation-constraints",
               strategy = "A ConstraintViolationException from failed validation is routed to a matching @HandleException method")
    public void invalidInputRoutedToHandler() {
        trace.reset();
        events.fire(new ValidationEvent(null));

        assertThat(trace.phases()).contains(Phase.HANDLE_EXCEPTION);
        TraceEntry handlerEntry = trace.entries().stream()
                .filter(e -> e.phase() == Phase.HANDLE_EXCEPTION)
                .findFirst()
                .orElseThrow();
        assertThat(handlerEntry.args()[0]).isInstanceOf(ConstraintViolationException.class);
    }

    // ── Invalid input with no handler: propagates to the container ───────────

    @RequiresImplementation
    @Assertion(id = "AGENTICAI-VALIDATION-BHV-004",
               section = "jakarta-validation-constraints",
               strategy = "Without a matching @HandleException, the ConstraintViolationException propagates to the container")
    public void invalidInputPropagatesWithoutHandler() {
        trace.reset();
        // CDI wraps observer exceptions; the cause is the ConstraintViolationException
        assertThatThrownBy(() -> unhandledEvents.fire(new UnhandledValidationEvent(null)))
                .hasRootCauseInstanceOf(ConstraintViolationException.class);
        assertThat(trace.phases()).doesNotContain(Phase.ACTION);
    }
}