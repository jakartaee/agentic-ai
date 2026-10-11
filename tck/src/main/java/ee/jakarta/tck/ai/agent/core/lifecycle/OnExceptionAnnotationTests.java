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
package ee.jakarta.tck.ai.agent.core.lifecycle;

import ee.jakarta.tck.ai.agent.framework.junit.anno.Assertion;
import ee.jakarta.tck.ai.agent.framework.junit.anno.Standalone;
import jakarta.ai.agent.Agent;
import jakarta.ai.agent.OnException;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TCK tests for the {@link OnException} annotation.
 *
 * <p>These tests verify that the @OnException annotation conforms to the
 * Jakarta Agentic AI 1.0 specification requirements. The @OnException annotation
 * marks methods that handle errors occurring during workflow execution.
 */
@Standalone
public class OnExceptionAnnotationTests {

    @Assertion(id = "AGENTICAI-ONEXCEPTION-001",
               strategy = "Verify @OnException annotation exists in the jakarta.ai.agent package")
    public void testOnExceptionAnnotationExists() {
        assertNotNull(OnException.class,
                "@OnException annotation must exist in jakarta.ai.agent package");
    }

    @Assertion(id = "AGENTICAI-ONEXCEPTION-002",
               strategy = "Verify @OnException annotation has RUNTIME retention policy")
    public void testOnExceptionAnnotationRetention() {
        Retention retention = OnException.class.getAnnotation(Retention.class);
        assertNotNull(retention,
                "@OnException must have @Retention annotation");
        assertEquals(RetentionPolicy.RUNTIME, retention.value(),
                "@OnException must have RUNTIME retention policy");
    }

    @Assertion(id = "AGENTICAI-ONEXCEPTION-003",
               strategy = "Verify @OnException annotation targets METHOD elements")
    public void testOnExceptionAnnotationTarget() {
        Target target = OnException.class.getAnnotation(Target.class);
        assertNotNull(target,
                "@OnException must have @Target annotation");

        ElementType[] targets = target.value();
        assertEquals(1, targets.length,
                "@OnException must target exactly one element type");
        assertEquals(ElementType.METHOD, targets[0],
                "@OnException must target METHOD elements");
    }

    @Assertion(id = "AGENTICAI-ONEXCEPTION-004",
               strategy = "Verify @OnException annotation is an annotation type")
    public void testOnExceptionIsAnnotationType() {
        assertTrue(OnException.class.isAnnotation(),
                "@OnException must be an annotation type");
    }

    @Assertion(id = "AGENTICAI-ONEXCEPTION-005",
               strategy = "Verify @OnException can be applied to a void method that handles exceptions")
    public void testOnExceptionCanBeAppliedToMethod() throws NoSuchMethodException {
        @Agent
        class TestAgent {
            @OnException
            public void onError(Exception exception) {}
        }

        Method method = TestAgent.class.getMethod("onError", Exception.class);
        OnException annotation = method.getAnnotation(OnException.class);
        assertNotNull(annotation,
                "@OnException annotation must be retrievable from annotated method");
        assertEquals(void.class, method.getReturnType(),
                "@OnException method must return void");
    }

    @Assertion(id = "AGENTICAI-ONEXCEPTION-006",
               strategy = "Verify @OnException method must return void")
    public void testOnExceptionMustReturnVoid() throws NoSuchMethodException {
        @Agent
        class TestAgent {
            @OnException
            public void handleError(Exception ex) {
                // Void return for exception handling
            }
        }

        Method method = TestAgent.class.getMethod("handleError", Exception.class);
        assertEquals(void.class, method.getReturnType(),
                "@OnException method must declare void return type");
    }
}
