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

/**
 * Selects implementation-dependent or plain-CDI baseline tests.
 *
 * <p>The TCK tests a compatible implementation by default. This fail-closed
 * default prevents an omitted configuration flag from silently skipping every
 * {@link RequiresImplementation} assertion and reporting false compliance.
 * A run with no compatible implementation fails those assertions; there is no
 * separate "implementation present" flag and no crafted missing-implementation
 * message. CDI 4.0 provides no portable way to probe for a registered
 * workflow context, so selection stays a declared opt-out rather than a
 * structural detection.</p>
 *
 * <p>The project plain-CDI baseline is selected only with the {@code baseline}
 * Maven profile ({@code -Pbaseline}). That profile sets
 * {@value #BASELINE_PROPERTY}, which Failsafe passes into the test JVM.
 * Baseline mode enables {@link RequiresNoImplementation} assertions and
 * disables {@link RequiresImplementation} assertions.</p>
 *
 * @see RequiresImplementation
 * @see RequiresNoImplementation
 */
public class ImplementationPresentCondition implements ExecutionCondition {

    /**
     * System property set by the {@code baseline} Maven profile. Not a
     * supported command-line substitute for {@code -Pbaseline}: Maven does not
     * forward an arbitrary {@code -D} of this name into Failsafe unless the
     * profile property {@code tck.baseline} is set.
     */
    public static final String BASELINE_PROPERTY = "jakarta.ai.agent.tck.baseline";

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        boolean requiresImplementation = AnnotationSupport.isAnnotated(
                context.getElement(), RequiresImplementation.class);
        boolean requiresNoImplementation = AnnotationSupport.isAnnotated(
                context.getElement(), RequiresNoImplementation.class);

        if (!requiresImplementation && !requiresNoImplementation) {
            return ConditionEvaluationResult.enabled("No implementation condition present");
        }

        boolean runAgainstImplementation = runAgainstImplementation(
                Boolean.getBoolean(BASELINE_PROPERTY));

        if (requiresImplementation) {
            return runAgainstImplementation
                    ? ConditionEvaluationResult.enabled(
                            "Implementation mode is the TCK default")
                    : ConditionEvaluationResult.disabled(
                            "Plain-CDI baseline explicitly selected");
        }

        return runAgainstImplementation
                ? ConditionEvaluationResult.disabled(
                        "Plain-CDI baseline assertions are disabled in implementation mode")
                : ConditionEvaluationResult.enabled(
                        "Plain-CDI baseline explicitly selected");
    }

    /**
     * Resolves the effective mode without container or system-property access.
     *
     * @param baselineRequested whether the plain-CDI baseline was explicitly selected
     * @return {@code true} for the default implementation mode
     */
    static boolean runAgainstImplementation(boolean baselineRequested) {
        return !baselineRequested;
    }
}
