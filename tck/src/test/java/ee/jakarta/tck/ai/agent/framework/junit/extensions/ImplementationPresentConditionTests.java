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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the Issue 51 run-mode selection.
 *
 * <p>These exercise the TCK's own infrastructure, not the specification.
 * They are tagged {@code internal} so they do not inflate specification
 * assertion counts.</p>
 */
@Tag("internal")
public class ImplementationPresentConditionTests {

    @Test
    void implementationTestIsEnabledByDefault() throws Exception {
        ConditionEvaluationResult result = evaluate("implementationTest", false);
        assertFalse(result.isDisabled(),
                "implementation assertions must run when baseline is not selected");
    }

    @Test
    void implementationTestIsDisabledInBaselineMode() throws Exception {
        ConditionEvaluationResult result = evaluate("implementationTest", true);
        assertTrue(result.isDisabled(),
                "implementation assertions must skip in explicit baseline mode");
    }

    @Test
    void baselineTestIsDisabledByDefault() throws Exception {
        ConditionEvaluationResult result = evaluate("baselineTest", false);
        assertTrue(result.isDisabled(),
                "plain-CDI assertions must skip in default implementation mode");
    }

    @Test
    void baselineTestIsEnabledInBaselineMode() throws Exception {
        ConditionEvaluationResult result = evaluate("baselineTest", true);
        assertFalse(result.isDisabled(),
                "plain-CDI assertions must run in explicit baseline mode");
    }

    @Test
    void unannotatedTestIsAlwaysEnabled() throws Exception {
        assertFalse(evaluate("unannotatedTest", false).isDisabled());
        assertFalse(evaluate("unannotatedTest", true).isDisabled());
    }

    @Test
    void baselineMarkerIsAbsentWithoutTheProfile() {
        assertFalse(ImplementationPresentCondition.baselineProfileActive(),
                "a Maven -D must not create the baseline classpath marker");
    }

    private static ConditionEvaluationResult evaluate(
            String methodName, boolean baselineRequested) throws Exception {
        Method method = ConditionFixtures.class.getDeclaredMethod(methodName);
        return ImplementationPresentCondition.evaluateCondition(
                Optional.of(method), baselineRequested);
    }

    private static final class ConditionFixtures {

        @RequiresImplementation
        void implementationTest() {
        }

        @RequiresNoImplementation
        void baselineTest() {
        }

        void unannotatedTest() {
        }
    }
}
