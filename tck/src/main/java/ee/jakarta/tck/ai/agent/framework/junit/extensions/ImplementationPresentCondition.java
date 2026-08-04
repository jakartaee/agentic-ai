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
package ee.jakarta.tck.ai.agent.framework.junit.extensions;

import ee.jakarta.tck.ai.agent.framework.junit.anno.RequiresImplementation;
import ee.jakarta.tck.ai.agent.framework.junit.anno.RequiresNoImplementation;
import jakarta.enterprise.inject.spi.CDI;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

import java.util.Locale;

/**
 * Enables or disables a test based on whether a compatible implementation of
 * the Agentic AI specification is present.
 *
 * <p>Detection is performed at runtime, inside the CDI container, by reading
 * the system property {@value #IMPLEMENTATION_PRESENT_PROPERTY}. Implementations
 * running the TCK must set this property to {@code true} (for example via
 * {@code -Djakarta.ai.agent.tck.implementation.present=true} on the Surefire
 * / Failsafe {@code argLine}) so that the {@link RequiresImplementation}
 * behavior assertions are exercised and the {@link RequiresNoImplementation}
 * baseline assertions are skipped. The default (unset) means "no compatible
 * implementation" — appropriate for a plain-CDI (Weld/OpenWebBeans) run of
 * the standalone assertions.</p>
 *
 * <p>An optional expected-mode property {@value #TCK_MODE_PROPERTY} may be set
 * to {@code implementation} or {@code baseline}. When set, the declared mode is
 * checked against the presence flag inside the container so a misconfigured run
 * fails fast instead of silently skipping the behavioral suite. When unset,
 * today's default baseline behavior is preserved.</p>
 *
 * <p>A system-property opt-in is used rather than a CDI-container probe so
 * this detector remains portable across CDI 4.0 (Jakarta EE 10) containers,
 * where the {@code BeanManager} does not expose a way to enumerate registered
 * contexts.</p>
 *
 * <p>When evaluated outside a CDI container (the Arquillian client JVM, where
 * conditions are also evaluated before a test is dispatched to the container)
 * a compatible implementation's presence cannot be determined, so the test is
 * left enabled and the real decision is deferred to the in-container
 * evaluation.</p>
 *
 * @see RequiresImplementation
 * @see RequiresNoImplementation
 */
public class ImplementationPresentCondition implements ExecutionCondition {

    /**
     * System property implementations must set to {@code true} when running
     * the TCK against a compatible implementation.
     */
    public static final String IMPLEMENTATION_PRESENT_PROPERTY =
            "jakarta.ai.agent.tck.implementation.present";

    /**
     * Optional system property declaring the expected TCK run mode.
     * Accepted values are {@code implementation} and {@code baseline}
     * (case-insensitive). When unset, the default baseline behavior is preserved.
     */
    public static final String TCK_MODE_PROPERTY =
            "jakarta.ai.agent.tck.mode";

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        boolean requiresImplementation = AnnotationSupport.isAnnotated(context.getElement(), RequiresImplementation.class);
        boolean requiresNoImplementation = AnnotationSupport.isAnnotated(context.getElement(), RequiresNoImplementation.class);
        if (!requiresImplementation && !requiresNoImplementation) {
            return ConditionEvaluationResult.enabled("No implementation condition present");
        }

        Boolean implementationPresent = detectImplementation();
        if (implementationPresent == null) {
            return ConditionEvaluationResult.enabled(
                    "Implementation presence undetermined outside the container; deferring to the in-container evaluation");
        }

        checkMode(System.getProperty(TCK_MODE_PROPERTY), implementationPresent);

        if (requiresImplementation) {
            return implementationPresent
                ? ConditionEvaluationResult.enabled("Agentic AI compatible implementation is present")
                : ConditionEvaluationResult.disabled("Requires an Agentic AI compatible implementation "
                            + "to dispatch @Decision/@Action/@Outcome phases");
        }
        // requiresNoImplementation
        return implementationPresent
                ? ConditionEvaluationResult.disabled("No-implementation baseline precondition; superseded by the "
                + "@RequiresImplementation behavior assertion when a compatible implementation is present")
                : ConditionEvaluationResult.enabled("No Agentic AI implementation present (plain-CDI baseline)");
    }

    /**
     * Validates that an optional declared TCK mode is consistent with the
     * detected implementation presence.
     *
     * <p>Package-private and free of system-property / CDI access so unit tests
     * can exercise the guard directly.</p>
     *
     * @param mode                   value of {@value #TCK_MODE_PROPERTY}, or {@code null}/{@code blank} when unset
     * @param implementationPresent  whether a compatible implementation is declared present
     * @throws IllegalStateException when the declared mode conflicts with presence, or the value is unknown
     */
    static void checkMode(String mode, boolean implementationPresent) {
        if (mode == null || mode.isBlank()) {
            return;
        }
        String normalized = mode.trim().toLowerCase(Locale.ROOT);
        switch (normalized) {
            case "implementation":
                if (!implementationPresent) {
                    throw new IllegalStateException(
                            "TCK mode is 'implementation' but "
                                    + IMPLEMENTATION_PRESENT_PROPERTY
                                    + " is not true; set -D"
                                    + IMPLEMENTATION_PRESENT_PROPERTY
                                    + "=true or omit -D"
                                    + TCK_MODE_PROPERTY);
                }
                return;
            case "baseline":
                if (implementationPresent) {
                    throw new IllegalStateException(
                            "TCK mode is 'baseline' but "
                                    + IMPLEMENTATION_PRESENT_PROPERTY
                                    + " is true; unset the presence flag or set -D"
                                    + TCK_MODE_PROPERTY
                                    + "=implementation");
                }
                return;
            default:
                throw new IllegalStateException(
                        "Unknown value for "
                                + TCK_MODE_PROPERTY
                                + ": '"
                                + mode
                                + "'. Accepted values are 'implementation' and 'baseline'");
        }
    }

    /**
     * @return {@code TRUE}/{@code FALSE} when the implementation's presence can be
     *         resolved from the current CDI container, or {@code null} when no
     *         container is available so the decision must be deferred.
     */
    private static Boolean detectImplementation() {
        try {
            // Ensure we are inside a CDI container. Outside one (e.g. the
            // Arquillian client JVM), CDI.current() throws IllegalStateException
            // and we defer the decision to the in-container evaluation.
            CDI.current();
        } catch (Throwable outsideContainer) {
            return null;
        }
        return Boolean.getBoolean(IMPLEMENTATION_PRESENT_PROPERTY);
    }
}
