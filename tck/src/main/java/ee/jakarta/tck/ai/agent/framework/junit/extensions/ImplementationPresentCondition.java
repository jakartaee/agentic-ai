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
import jakarta.ai.agent.WorkflowScoped;
import jakarta.enterprise.inject.spi.CDI;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

/**
 * Enables or disables a test based on whether a compatible implementation of
 * the Agentic AI specification is present, with no configuration required.
 *
 * <p>Detection is performed at runtime, inside the CDI container, by checking
 * whether a {@link WorkflowScoped} {@code Context} has been registered — every
 * compatible implementation registers one, and plain CDI (no implementation)
 * does not. This works for any vendor without a system property or server JVM
 * option.</p>
 *
 * <p>When evaluated outside a CDI container (the Arquillian client JVM, where
 * conditions are also evaluated before a test is dispatched to the container)
 * a compatible implementation's presence cannot be determined, so the test is left enabled and
 * the real decision is deferred to the in-container evaluation.</p>
 *
 * @see RequiresImplementation
 * @see RequiresNoImplementation
 */
public class ImplementationPresentCondition implements ExecutionCondition {

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
     * @return {@code TRUE}/{@code FALSE} when the implementation's presence can be
     *         resolved from the current CDI container, or {@code null} when no
     *         container is available so the decision must be deferred.
     */
    private static Boolean detectImplementation() {
        try {
            return !CDI.current().getBeanManager().getContexts(WorkflowScoped.class).isEmpty();
        } catch (Throwable outsideContainer) {
            return null;
        }
    }
}