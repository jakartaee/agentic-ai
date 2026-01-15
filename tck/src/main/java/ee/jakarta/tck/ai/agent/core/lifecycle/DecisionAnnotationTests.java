/*
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package ee.jakarta.tck.ai.agent.core.lifecycle;

import ee.jakarta.tck.ai.agent.framework.junit.anno.Assertion;
import jakarta.ai.agent.Agent;
import jakarta.ai.agent.Decision;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TCK tests for the {@link Decision} annotation.
 *
 * <p>These tests verify that the @Decision annotation conforms to the
 * Jakarta Agentic AI 1.0 specification requirements. The @Decision annotation
 * marks methods that evaluate conditions and determine workflow continuation.
 */
public class DecisionAnnotationTests {

    @Assertion(id = "AGENTICAI-DECISION-001",
               strategy = "Verify @Decision annotation exists in the jakarta.ai.agent package")
    public void testDecisionAnnotationExists() {
        assertNotNull(Decision.class,
                "@Decision annotation must exist in jakarta.ai.agent package");
    }

    @Assertion(id = "AGENTICAI-DECISION-002",
               strategy = "Verify @Decision annotation has RUNTIME retention policy")
    public void testDecisionAnnotationRetention() {
        Retention retention = Decision.class.getAnnotation(Retention.class);
        assertNotNull(retention,
                "@Decision must have @Retention annotation");
        assertEquals(RetentionPolicy.RUNTIME, retention.value(),
                "@Decision must have RUNTIME retention policy");
    }

    @Assertion(id = "AGENTICAI-DECISION-003",
               strategy = "Verify @Decision annotation targets METHOD elements")
    public void testDecisionAnnotationTarget() {
        Target target = Decision.class.getAnnotation(Target.class);
        assertNotNull(target,
                "@Decision must have @Target annotation");

        ElementType[] targets = target.value();
        assertEquals(1, targets.length,
                "@Decision must target exactly one element type");
        assertEquals(ElementType.METHOD, targets[0],
                "@Decision must target METHOD elements");
    }

    @Assertion(id = "AGENTICAI-DECISION-004",
               strategy = "Verify @Decision annotation is an annotation type")
    public void testDecisionIsAnnotationType() {
        assertTrue(Decision.class.isAnnotation(),
                "@Decision must be an annotation type");
    }

    @Assertion(id = "AGENTICAI-DECISION-005",
               strategy = "Verify @Decision can be applied to a method returning boolean")
    public void testDecisionCanBeAppliedToBooleanMethod() throws NoSuchMethodException {
        @Agent
        class TestAgent {
            @Decision
            public boolean shouldProceed(Object data) {
                return true;
            }
        }

        Method method = TestAgent.class.getMethod("shouldProceed", Object.class);
        Decision annotation = method.getAnnotation(Decision.class);
        assertNotNull(annotation,
                "@Decision annotation must be retrievable from annotated method");
        assertEquals(boolean.class, method.getReturnType(),
                "@Decision method can return boolean");
    }

    @Assertion(id = "AGENTICAI-DECISION-006",
               strategy = "Verify @Decision can be applied to a method returning Object")
    public void testDecisionCanBeAppliedToObjectMethod() throws NoSuchMethodException {
        @Agent
        class TestAgent {
            @Decision
            public Object analyzeAndDecide(Object data) {
                return new Object();
            }
        }

        Method method = TestAgent.class.getMethod("analyzeAndDecide", Object.class);
        Decision annotation = method.getAnnotation(Decision.class);
        assertNotNull(annotation,
                "@Decision annotation must be retrievable from annotated method returning Object");
    }
}
