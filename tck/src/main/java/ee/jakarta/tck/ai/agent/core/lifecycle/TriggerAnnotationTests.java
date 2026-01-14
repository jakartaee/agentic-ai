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
import jakarta.ai.agent.Trigger;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TCK tests for the {@link Trigger} annotation.
 *
 * <p>These tests verify that the @Trigger annotation conforms to the
 * Jakarta Agentic AI 1.0 specification requirements. The @Trigger annotation
 * marks methods that initiate an agent workflow in response to external events.
 */
public class TriggerAnnotationTests {

    @Assertion(id = "AGENTICAI-TRIGGER-001",
               strategy = "Verify @Trigger annotation exists in the jakarta.ai.agent package")
    public void testTriggerAnnotationExists() {
        assertNotNull(Trigger.class,
                "@Trigger annotation must exist in jakarta.ai.agent package");
    }

    @Assertion(id = "AGENTICAI-TRIGGER-002",
               strategy = "Verify @Trigger annotation has RUNTIME retention policy")
    public void testTriggerAnnotationRetention() {
        Retention retention = Trigger.class.getAnnotation(Retention.class);
        assertNotNull(retention,
                "@Trigger must have @Retention annotation");
        assertEquals(RetentionPolicy.RUNTIME, retention.value(),
                "@Trigger must have RUNTIME retention policy");
    }

    @Assertion(id = "AGENTICAI-TRIGGER-003",
               strategy = "Verify @Trigger annotation targets METHOD elements")
    public void testTriggerAnnotationTarget() {
        Target target = Trigger.class.getAnnotation(Target.class);
        assertNotNull(target,
                "@Trigger must have @Target annotation");

        ElementType[] targets = target.value();
        assertEquals(1, targets.length,
                "@Trigger must target exactly one element type");
        assertEquals(ElementType.METHOD, targets[0],
                "@Trigger must target METHOD elements");
    }

    @Assertion(id = "AGENTICAI-TRIGGER-004",
               strategy = "Verify @Trigger annotation is an annotation type")
    public void testTriggerIsAnnotationType() {
        assertTrue(Trigger.class.isAnnotation(),
                "@Trigger must be an annotation type");
    }

    @Assertion(id = "AGENTICAI-TRIGGER-005",
               strategy = "Verify @Trigger can be applied to a method in an @Agent class")
    public void testTriggerCanBeAppliedToMethod() throws NoSuchMethodException {
        @Agent
        class TestAgent {
            @Trigger
            public void onEvent(Object event) {}
        }

        Method method = TestAgent.class.getMethod("onEvent", Object.class);
        Trigger annotation = method.getAnnotation(Trigger.class);
        assertNotNull(annotation,
                "@Trigger annotation must be retrievable from annotated method");
    }
}
