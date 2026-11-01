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
                        UnhandledValidatedAgent.class, UnhandledValidationEvent.class
                )
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject Event<ValidationEvent>          events;
    @Inject Event<UnhandledValidationEvent> unhandledEvents;
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
        // CDI wraps observer exceptions; the cause is the ConstraintViolationException
        Throwable thrown = assertThrows(Throwable.class,
                () -> unhandledEvents.fire(new UnhandledValidationEvent(null)));
        Throwable rootCause = thrown;
        while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }
        assertInstanceOf(ConstraintViolationException.class, rootCause);
        assertFalse(trace.phases().contains(Phase.ACTION));
    }
}