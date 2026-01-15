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
import jakarta.ai.agent.HandleException;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TCK tests for the {@link HandleException} annotation.
 *
 * <p>These tests verify that the @HandleException annotation conforms to the
 * Jakarta Agentic AI 1.0 specification requirements. The @HandleException annotation
 * marks methods that handle errors occurring during workflow execution.
 */
public class HandleExceptionAnnotationTests {

    @Assertion(id = "AGENTICAI-HANDLEEXCEPTION-001",
               strategy = "Verify @HandleException annotation exists in the jakarta.ai.agent package")
    public void testHandleExceptionAnnotationExists() {
        assertNotNull(HandleException.class,
                "@HandleException annotation must exist in jakarta.ai.agent package");
    }

    @Assertion(id = "AGENTICAI-HANDLEEXCEPTION-002",
               strategy = "Verify @HandleException annotation has RUNTIME retention policy")
    public void testHandleExceptionAnnotationRetention() {
        Retention retention = HandleException.class.getAnnotation(Retention.class);
        assertNotNull(retention,
                "@HandleException must have @Retention annotation");
        assertEquals(RetentionPolicy.RUNTIME, retention.value(),
                "@HandleException must have RUNTIME retention policy");
    }

    @Assertion(id = "AGENTICAI-HANDLEEXCEPTION-003",
               strategy = "Verify @HandleException annotation targets METHOD elements")
    public void testHandleExceptionAnnotationTarget() {
        Target target = HandleException.class.getAnnotation(Target.class);
        assertNotNull(target,
                "@HandleException must have @Target annotation");

        ElementType[] targets = target.value();
        assertEquals(1, targets.length,
                "@HandleException must target exactly one element type");
        assertEquals(ElementType.METHOD, targets[0],
                "@HandleException must target METHOD elements");
    }

    @Assertion(id = "AGENTICAI-HANDLEEXCEPTION-004",
               strategy = "Verify @HandleException annotation is an annotation type")
    public void testHandleExceptionIsAnnotationType() {
        assertTrue(HandleException.class.isAnnotation(),
                "@HandleException must be an annotation type");
    }

    @Assertion(id = "AGENTICAI-HANDLEEXCEPTION-005",
               strategy = "Verify @HandleException can be applied to a method that handles exceptions")
    public void testHandleExceptionCanBeAppliedToMethod() throws NoSuchMethodException {
        @Agent
        class TestAgent {
            @HandleException
            public void onError(Exception exception) {}
        }

        Method method = TestAgent.class.getMethod("onError", Exception.class);
        HandleException annotation = method.getAnnotation(HandleException.class);
        assertNotNull(annotation,
                "@HandleException annotation must be retrievable from annotated method");
    }
}
