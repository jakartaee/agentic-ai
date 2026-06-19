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

import ee.jakarta.tck.ai.agent.framework.junit.anno.RequiresEngine;
import ee.jakarta.tck.ai.agent.framework.junit.anno.RequiresNoEngine;
import jakarta.ai.agent.WorkflowScoped;
import jakarta.enterprise.inject.spi.CDI;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

/**
 * Enables or disables a test based on whether a Reference Implementation of the
 * Agentic AI engine is present, with no configuration required.
 *
 * <p>Detection is performed at runtime, inside the CDI container, by checking
 * whether a {@link WorkflowScoped} {@code Context} has been registered — every
 * compliant Reference Implementation registers one, and plain CDI (no engine)
 * does not. This works for any vendor without a system property or server JVM
 * option.</p>
 *
 * <p>When evaluated outside a CDI container (the Arquillian client JVM, where
 * conditions are also evaluated before a test is dispatched to the container)
 * the engine's presence cannot be determined, so the test is left enabled and
 * the real decision is deferred to the in-container evaluation.</p>
 *
 * @see RequiresEngine
 * @see RequiresNoEngine
 */
public class EngineCondition implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        boolean requiresEngine = AnnotationSupport.isAnnotated(context.getElement(), RequiresEngine.class);
        boolean requiresNoEngine = AnnotationSupport.isAnnotated(context.getElement(), RequiresNoEngine.class);
        if (!requiresEngine && !requiresNoEngine) {
            return ConditionEvaluationResult.enabled("No engine condition present");
        }

        Boolean enginePresent = detectEngine();
        if (enginePresent == null) {
            return ConditionEvaluationResult.enabled(
                    "Engine presence undetermined outside the container; deferring to the in-container evaluation");
        }

        if (requiresEngine) {
            return enginePresent
                    ? ConditionEvaluationResult.enabled("Agentic AI engine (Reference Implementation) is present")
                    : ConditionEvaluationResult.disabled("Requires a Reference Implementation of the Agentic AI engine "
                            + "to dispatch @Decision/@Action/@Outcome phases");
        }
        // requiresNoEngine
        return enginePresent
                ? ConditionEvaluationResult.disabled("No-engine baseline precondition; superseded by the "
                        + "@RequiresEngine behavior assertion when a Reference Implementation is present")
                : ConditionEvaluationResult.enabled("No Agentic AI engine present (plain-CDI baseline)");
    }

    /**
     * @return {@code TRUE}/{@code FALSE} when the engine's presence can be
     *         resolved from the current CDI container, or {@code null} when no
     *         container is available so the decision must be deferred.
     */
    private static Boolean detectEngine() {
        try {
            return !CDI.current().getBeanManager().getContexts(WorkflowScoped.class).isEmpty();
        } catch (Throwable outsideContainer) {
            return null;
        }
    }
}