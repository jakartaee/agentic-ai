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
import jakarta.ai.agent.Outcome;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TCK tests for the {@link Outcome} annotation.
 *
 * <p>These tests verify that the @Outcome annotation conforms to the
 * Jakarta Agentic AI 1.0 specification requirements. The @Outcome annotation
 * marks methods that produce the final result of a workflow and handle
 * completion logic.
 */
public class OutcomeAnnotationTests {

    @Assertion(id = "AGENTICAI-OUTCOME-001",
               strategy = "Verify @Outcome annotation exists in the jakarta.ai.agent package")
    public void testOutcomeAnnotationExists() {
        assertNotNull(Outcome.class,
                "@Outcome annotation must exist in jakarta.ai.agent package");
    }

    @Assertion(id = "AGENTICAI-OUTCOME-002",
               strategy = "Verify @Outcome annotation has RUNTIME retention policy")
    public void testOutcomeAnnotationRetention() {
        Retention retention = Outcome.class.getAnnotation(Retention.class);
        assertNotNull(retention,
                "@Outcome must have @Retention annotation");
        assertEquals(RetentionPolicy.RUNTIME, retention.value(),
                "@Outcome must have RUNTIME retention policy");
    }

    @Assertion(id = "AGENTICAI-OUTCOME-003",
               strategy = "Verify @Outcome annotation targets METHOD elements")
    public void testOutcomeAnnotationTarget() {
        Target target = Outcome.class.getAnnotation(Target.class);
        assertNotNull(target,
                "@Outcome must have @Target annotation");

        ElementType[] targets = target.value();
        assertEquals(1, targets.length,
                "@Outcome must target exactly one element type");
        assertEquals(ElementType.METHOD, targets[0],
                "@Outcome must target METHOD elements");
    }

    @Assertion(id = "AGENTICAI-OUTCOME-004",
               strategy = "Verify @Outcome annotation is an annotation type")
    public void testOutcomeIsAnnotationType() {
        assertTrue(Outcome.class.isAnnotation(),
                "@Outcome must be an annotation type");
    }

    @Assertion(id = "AGENTICAI-OUTCOME-005",
               strategy = "Verify @Outcome can be applied to a method in an @Agent class")
    public void testOutcomeCanBeAppliedToMethod() throws NoSuchMethodException {
        @Agent
        class TestAgent {
            @Outcome
            public Object produceResult() {
                return new Object();
            }
        }

        Method method = TestAgent.class.getMethod("produceResult");
        Outcome annotation = method.getAnnotation(Outcome.class);
        assertNotNull(annotation,
                "@Outcome annotation must be retrievable from annotated method");
    }
}
