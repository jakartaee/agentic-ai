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
        assertTrue(WorkflowContext.class.isInterface(),
                "WorkflowContext must be an interface");
        assertEquals(PACKAGE_NAME, WorkflowContext.class.getPackageName(),
                "WorkflowContext must be in " + PACKAGE_NAME);
    }

    @Assertion(id = "AGENTICAI-SIG-003",
               strategy = "Verify WorkflowContext interface has all required methods")
    public void testWorkflowContextSignature() {
        Set<String> requiredMethods = new HashSet<>(Arrays.asList(
                "setAttribute",
                "getAttribute",
                "removeAttribute",
                "getAttributeNames",
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

    @Assertion(id = "AGENTICAI-SIG-006",
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

    @Assertion(id = "AGENTICAI-SIG-007",
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

    @Assertion(id = "AGENTICAI-SIG-008",
               strategy = "Verify Result record exists in the API with correct structure")
    public void testResultRecordSignature() {
        // Verify Result class exists
        assertNotNull(Result.class, "Result class must exist in jakarta.ai.agent package");
        assertEquals(PACKAGE_NAME, Result.class.getPackageName(),
                "Result must be in " + PACKAGE_NAME);

        // Verify Result is a record
        assertTrue(Result.class.isRecord(),
                "Result must be a record type");

        // Verify record components
        var components = Result.class.getRecordComponents();
        assertNotNull(components, "Result record must have components");
        assertEquals(2, components.length, "Result record must have exactly 2 components");

        // Verify success component (boolean)
        assertTrue(Arrays.stream(components).anyMatch(c ->
                c.getName().equals("success") && c.getType().equals(boolean.class)),
                "Result must have 'success' component of type boolean");

        // Verify details component (Object)
        assertTrue(Arrays.stream(components).anyMatch(c ->
                c.getName().equals("details") && c.getType().equals(Object.class)),
                "Result must have 'details' component of type Object");
    }

    @Assertion(id = "AGENTICAI-SIG-009",
               strategy = "Verify LLMException exists and extends RuntimeException")
    public void testLLMExceptionSignature() {
        try {
            Class<?> llmExceptionClass = Class.forName("jakarta.ai.agent.LLMException");
            assertNotNull(llmExceptionClass, "LLMException class must exist in jakarta.ai.agent package");
            assertEquals(PACKAGE_NAME, llmExceptionClass.getPackageName(),
                    "LLMException must be in " + PACKAGE_NAME);

            // Verify it extends RuntimeException
            assertTrue(RuntimeException.class.isAssignableFrom(llmExceptionClass),
                    "LLMException must extend RuntimeException");

            // Verify constructors exist
            boolean hasDefaultConstructor = Arrays.stream(llmExceptionClass.getDeclaredConstructors())
                    .anyMatch(c -> c.getParameterCount() == 0);
            assertTrue(hasDefaultConstructor, "LLMException must have a no-arg constructor");

            boolean hasStringConstructor = Arrays.stream(llmExceptionClass.getDeclaredConstructors())
                    .anyMatch(c -> c.getParameterCount() == 1 && c.getParameterTypes()[0].equals(String.class));
            assertTrue(hasStringConstructor, "LLMException must have a constructor with String parameter");

            boolean hasStringThrowableConstructor = Arrays.stream(llmExceptionClass.getDeclaredConstructors())
                    .anyMatch(c -> c.getParameterCount() == 2 &&
                            c.getParameterTypes()[0].equals(String.class) &&
                            c.getParameterTypes()[1].equals(Throwable.class));
            assertTrue(hasStringThrowableConstructor,
                    "LLMException must have a constructor with String and Throwable parameters");

        } catch (ClassNotFoundException e) {
            fail("LLMException class not found: " + e.getMessage());
        }
    }

    @Assertion(id = "AGENTICAI-SIG-010",
               strategy = "Verify WorkflowScoped.Literal inner class exists")
    public void testWorkflowScopedLiteralInnerClass() {
        // Find the Literal inner class
        Class<?>[] innerClasses = WorkflowScoped.class.getDeclaredClasses();
        assertTrue(innerClasses.length > 0,
                "WorkflowScoped must have inner classes");

        boolean hasLiteral = Arrays.stream(innerClasses)
                .anyMatch(c -> c.getSimpleName().equals("Literal"));
        assertTrue(hasLiteral,
                "WorkflowScoped must have a Literal inner class");

        try {
            Class<?> literalClass = Class.forName("jakarta.ai.agent.WorkflowScoped$Literal");
            assertNotNull(literalClass,
                    "WorkflowScoped.Literal must be accessible");
        } catch (ClassNotFoundException e) {
            fail("WorkflowScoped.Literal class not found: " + e.getMessage());
        }
    }

    @Assertion(id = "AGENTICAI-SIG-011",
               strategy = "Verify LargeLanguageModel query methods have correct signatures")
    public void testLargeLanguageModelQueryMethodSignatures() {
        // Find query methods
        Method[] queryMethods = Arrays.stream(LargeLanguageModel.class.getDeclaredMethods())
                .filter(m -> m.getName().equals("query"))
                .toArray(Method[]::new);

        assertTrue(queryMethods.length >= 2,
                "LargeLanguageModel must have at least 2 query method overloads");

        // Verify query(String, Class<T>) exists
        boolean hasQueryStringClass = Arrays.stream(queryMethods)
                .anyMatch(m -> m.getParameterCount() == 2 &&
                        m.getParameterTypes()[0].equals(String.class) &&
                        m.getParameterTypes()[1].getName().contains("Class"));
        assertTrue(hasQueryStringClass,
                "LargeLanguageModel must have query(String, Class<T>) method");

        // Verify query(String, Object[]) exists
        boolean hasQueryStringObjects = Arrays.stream(queryMethods)
                .anyMatch(m -> m.getParameterCount() >= 2 &&
                        m.getParameterTypes()[0].equals(String.class));
        assertTrue(hasQueryStringObjects,
                "LargeLanguageModel must have query methods with String as first parameter");
    }

    @Assertion(id = "AGENTICAI-SIG-012",
               strategy = "Verify WorkflowContext methods have correct return types")
    public void testWorkflowContextMethodReturnTypes() {
        try {
            // setAttribute should return void
            Method setAttr = WorkflowContext.class.getMethod("setAttribute", String.class, Object.class);
            assertEquals(void.class, setAttr.getReturnType(),
                    "setAttribute must return void");

            // getAttribute should return Object
            Method getAttr = WorkflowContext.class.getMethod("getAttribute", String.class);
            assertEquals(Object.class, getAttr.getReturnType(),
                    "getAttribute must return Object");

            // removeAttribute should return void
            Method removeAttr = WorkflowContext.class.getMethod("removeAttribute", String.class);
            assertEquals(void.class, removeAttr.getReturnType(),
                    "removeAttribute must return void");

            // getTriggerEvent should return Object
            Method getTrigger = WorkflowContext.class.getMethod("getTriggerEvent");
            assertEquals(Object.class, getTrigger.getReturnType(),
                    "getTriggerEvent must return Object");

        } catch (NoSuchMethodException e) {
            fail("Required WorkflowContext method not found: " + e.getMessage());
        }
    }

    @Assertion(id = "AGENTICAI-SIG-012",
               strategy = "Verify @WorkflowScoped annotation has required meta-annotations")
    public void testWorkflowScopedMetaAnnotations() {
        // Check for @NormalScope
        jakarta.enterprise.context.NormalScope normalScope =
                WorkflowScoped.class.getAnnotation(jakarta.enterprise.context.NormalScope.class);
        assertNotNull(normalScope,
                "@WorkflowScoped must be annotated with @NormalScope");

        // Check for @Target
        java.lang.annotation.Target target =
                WorkflowScoped.class.getAnnotation(java.lang.annotation.Target.class);
        assertNotNull(target,
                "@WorkflowScoped must have @Target annotation");

        // Check for @Retention
        java.lang.annotation.Retention retention =
                WorkflowScoped.class.getAnnotation(java.lang.annotation.Retention.class);
        assertNotNull(retention,
                "@WorkflowScoped must have @Retention annotation");

        // Check for @Documented
        java.lang.annotation.Documented documented =
                WorkflowScoped.class.getAnnotation(java.lang.annotation.Documented.class);
        assertNotNull(documented,
                "@WorkflowScoped must be annotated with @Documented");

        // Check for @Inherited
        java.lang.annotation.Inherited inherited =
                WorkflowScoped.class.getAnnotation(java.lang.annotation.Inherited.class);
        assertNotNull(inherited,
                "@WorkflowScoped must be annotated with @Inherited");
    }
}
