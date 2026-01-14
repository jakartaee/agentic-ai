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
import jakarta.ai.agent.Action;
import jakarta.ai.agent.Agent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TCK tests for the {@link Action} annotation.
 *
 * <p>These tests verify that the @Action annotation conforms to the
 * Jakarta Agentic AI 1.0 specification requirements. The @Action annotation
 * marks methods that perform operations as part of the agent's workflow.
 */
public class ActionAnnotationTests {

    @Assertion(id = "AGENTICAI-ACTION-001",
               strategy = "Verify @Action annotation exists in the jakarta.ai.agent package")
    public void testActionAnnotationExists() {
        assertNotNull(Action.class,
                "@Action annotation must exist in jakarta.ai.agent package");
    }

    @Assertion(id = "AGENTICAI-ACTION-002",
               strategy = "Verify @Action annotation has RUNTIME retention policy")
    public void testActionAnnotationRetention() {
        Retention retention = Action.class.getAnnotation(Retention.class);
        assertNotNull(retention,
                "@Action must have @Retention annotation");
        assertEquals(RetentionPolicy.RUNTIME, retention.value(),
                "@Action must have RUNTIME retention policy");
    }

    @Assertion(id = "AGENTICAI-ACTION-003",
               strategy = "Verify @Action annotation targets METHOD elements")
    public void testActionAnnotationTarget() {
        Target target = Action.class.getAnnotation(Target.class);
        assertNotNull(target,
                "@Action must have @Target annotation");

        ElementType[] targets = target.value();
        assertEquals(1, targets.length,
                "@Action must target exactly one element type");
        assertEquals(ElementType.METHOD, targets[0],
                "@Action must target METHOD elements");
    }

    @Assertion(id = "AGENTICAI-ACTION-004",
               strategy = "Verify @Action annotation is an annotation type")
    public void testActionIsAnnotationType() {
        assertTrue(Action.class.isAnnotation(),
                "@Action must be an annotation type");
    }

    @Assertion(id = "AGENTICAI-ACTION-005",
               strategy = "Verify @Action can be applied to a method in an @Agent class")
    public void testActionCanBeAppliedToMethod() throws NoSuchMethodException {
        @Agent
        class TestAgent {
            @Action
            public void performAction(Object data) {}
        }

        Method method = TestAgent.class.getMethod("performAction", Object.class);
        Action annotation = method.getAnnotation(Action.class);
        assertNotNull(annotation,
                "@Action annotation must be retrievable from annotated method");
    }

    @Assertion(id = "AGENTICAI-ACTION-006",
               strategy = "Verify multiple @Action methods can exist in the same agent")
    public void testMultipleActionsInSameAgent() throws NoSuchMethodException {
        @Agent
        class TestAgent {
            @Action
            public void firstAction() {}

            @Action
            public void secondAction() {}
        }

        Method first = TestAgent.class.getMethod("firstAction");
        Method second = TestAgent.class.getMethod("secondAction");

        assertNotNull(first.getAnnotation(Action.class),
                "First @Action must be present");
        assertNotNull(second.getAnnotation(Action.class),
                "Second @Action must be present");
    }
}
