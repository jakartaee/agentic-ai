/*
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package ee.jakarta.tck.ai.agent.framework.signature;

import ee.jakarta.tck.ai.agent.framework.junit.anno.Assertion;
import jakarta.ai.agent.*;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Signature tests for the Jakarta Agentic AI 1.0 API.
 *
 * <p>These tests verify that the API surface matches the specification.
 * They ensure that all required types, methods, and annotations are present
 * with the correct signatures.
 */
public class SignatureTests {

    private static final String PACKAGE_NAME = "jakarta.ai.agent";

    @Assertion(id = "AGENTICAI-SIG-001",
               strategy = "Verify all required annotation types exist in the API")
    public void testRequiredAnnotationTypesExist() {
        Class<?>[] requiredAnnotations = {
                Agent.class,
                Trigger.class,
                Decision.class,
                Action.class,
                Outcome.class,
                HandleException.class,
                WorkflowScoped.class
        };

        for (Class<?> annotation : requiredAnnotations) {
            assertTrue(annotation.isAnnotation(),
                    annotation.getSimpleName() + " must be an annotation type");
            assertEquals(PACKAGE_NAME, annotation.getPackageName(),
                    annotation.getSimpleName() + " must be in " + PACKAGE_NAME);
        }
    }

    @Assertion(id = "AGENTICAI-SIG-002",
               strategy = "Verify all required interfaces exist in the API")
    public void testRequiredInterfacesExist() {
        assertTrue(LargeLanguageModel.class.isInterface(),
                "LargeLanguageModel must be an interface");
        assertEquals(PACKAGE_NAME, LargeLanguageModel.class.getPackageName(),
                "LargeLanguageModel must be in " + PACKAGE_NAME);
    }

    @Assertion(id = "AGENTICAI-SIG-003",
               strategy = "Verify all required classes exist in the API")
    public void testRequiredClassesExist() {
        assertFalse(WorkflowContext.class.isInterface(),
                "WorkflowContext must be a class");
        assertEquals(PACKAGE_NAME, WorkflowContext.class.getPackageName(),
                "WorkflowContext must be in " + PACKAGE_NAME);
    }

    @Assertion(id = "AGENTICAI-SIG-004",
               strategy = "Verify LargeLanguageModel interface has all required methods")
    public void testLargeLanguageModelSignature() {
        Set<String> requiredMethods = new HashSet<>(Arrays.asList(
                "query",
                "unwrap"
        ));

        Set<String> actualMethods = new HashSet<>();
        for (Method method : LargeLanguageModel.class.getDeclaredMethods()) {
            actualMethods.add(method.getName());
        }

        for (String required : requiredMethods) {
            assertTrue(actualMethods.contains(required),
                    "LargeLanguageModel must have method: " + required);
        }
    }

    @Assertion(id = "AGENTICAI-SIG-005",
               strategy = "Verify WorkflowContext class has all required methods")
    public void testWorkflowContextSignature() {
        Set<String> requiredMethods = new HashSet<>(Arrays.asList(
                "setAttribute",
                "getAttribute",
                "getTriggerEvent"
        ));

        Set<String> actualMethods = new HashSet<>();
        for (Method method : WorkflowContext.class.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers())) {
                actualMethods.add(method.getName());
            }
        }

        for (String required : requiredMethods) {
            assertTrue(actualMethods.contains(required),
                    "WorkflowContext must have public method: " + required);
        }
    }

    @Assertion(id = "AGENTICAI-SIG-006",
               strategy = "Verify lifecycle annotations target METHOD elements")
    public void testLifecycleAnnotationsTargetMethod() {
        Class<?>[] lifecycleAnnotations = {
                Trigger.class,
                Decision.class,
                Action.class,
                Outcome.class,
                HandleException.class
        };

        for (Class<?> annotation : lifecycleAnnotations) {
            java.lang.annotation.Target target = annotation.getAnnotation(java.lang.annotation.Target.class);
            assertNotNull(target,
                    annotation.getSimpleName() + " must have @Target annotation");

            boolean targetsMethod = Arrays.asList(target.value())
                    .contains(java.lang.annotation.ElementType.METHOD);
            assertTrue(targetsMethod,
                    annotation.getSimpleName() + " must target METHOD elements");
        }
    }

    @Assertion(id = "AGENTICAI-SIG-007",
               strategy = "Verify @Agent annotation targets TYPE elements")
    public void testAgentAnnotationTargetsType() {
        java.lang.annotation.Target target = Agent.class.getAnnotation(java.lang.annotation.Target.class);
        assertNotNull(target,
                "@Agent must have @Target annotation");

        boolean targetsType = Arrays.asList(target.value())
                .contains(java.lang.annotation.ElementType.TYPE);
        assertTrue(targetsType,
                "@Agent must target TYPE elements");
    }

    @Assertion(id = "AGENTICAI-SIG-008",
               strategy = "Verify all annotations have RUNTIME retention")
    public void testAllAnnotationsHaveRuntimeRetention() {
        Class<?>[] allAnnotations = {
                Agent.class,
                Trigger.class,
                Decision.class,
                Action.class,
                Outcome.class,
                HandleException.class,
                WorkflowScoped.class
        };

        for (Class<?> annotation : allAnnotations) {
            java.lang.annotation.Retention retention =
                    annotation.getAnnotation(java.lang.annotation.Retention.class);
            assertNotNull(retention,
                    annotation.getSimpleName() + " must have @Retention annotation");
            assertEquals(java.lang.annotation.RetentionPolicy.RUNTIME, retention.value(),
                    annotation.getSimpleName() + " must have RUNTIME retention");
        }
    }
}
