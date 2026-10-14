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
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

import java.lang.reflect.AnnotatedElement;
import java.util.Optional;

/**
 * Selects implementation-dependent or plain-CDI baseline tests.
 *
 * <p>The TCK tests a compatible implementation by default. This fail-closed
 * default prevents an omitted configuration flag from silently skipping every
 * {@link RequiresImplementation} assertion and reporting false compliance.
 * A run with no compatible implementation fails those assertions; there is no
 * separate "implementation present" flag and no crafted missing-implementation
 * message. CDI 4.0 provides no portable way to probe for a registered workflow
 * context, so selection uses an explicit baseline opt-out.</p>
 *
 * <p>The project plain-CDI baseline is selected only with the {@code baseline}
 * Maven profile ({@code -Pbaseline}). The profile passes an internal marker to
 * the test JVM. Baseline mode enables {@link RequiresNoImplementation}
 * assertions and disables {@link RequiresImplementation} assertions.</p>
 *
 * @see RequiresImplementation
 * @see RequiresNoImplementation
 */
public class ImplementationPresentCondition implements ExecutionCondition {

    /**
     * Internal test-JVM marker set by the {@code baseline} Maven profile.
     */
    public static final String BASELINE_PROPERTY =
            "jakarta.ai.agent.tck.internal.baseline-profile-active";

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        return evaluateCondition(
                context.getElement(),
                Boolean.getBoolean(BASELINE_PROPERTY));
    }

    /**
     * Resolves a condition without container or system-property access.
     *
     * @param element annotated test element
     * @param baselineRequested whether the plain-CDI baseline profile is active
     * @return the JUnit execution decision
     */
    static ConditionEvaluationResult evaluateCondition(
            Optional<? extends AnnotatedElement> element,
            boolean baselineRequested) {
        boolean requiresImplementation = AnnotationSupport.isAnnotated(
                element, RequiresImplementation.class);
        boolean requiresNoImplementation = AnnotationSupport.isAnnotated(
                element, RequiresNoImplementation.class);

        if (!requiresImplementation && !requiresNoImplementation) {
            return ConditionEvaluationResult.enabled("No implementation condition present");
        }

        if (requiresImplementation) {
            return baselineRequested
                    ? ConditionEvaluationResult.disabled(
                            "Plain-CDI baseline explicitly selected")
                    : ConditionEvaluationResult.enabled(
                            "Implementation mode is the TCK default");
        }

        return baselineRequested
                ? ConditionEvaluationResult.enabled(
                        "Plain-CDI baseline explicitly selected")
                : ConditionEvaluationResult.disabled(
                        "Plain-CDI baseline assertions are disabled in implementation mode");
    }
}
