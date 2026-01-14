/*
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package ee.jakarta.tck.ai.agent.core.agent;

import ee.jakarta.tck.ai.agent.framework.junit.anno.Assertion;
import jakarta.ai.agent.WorkflowContext;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TCK tests for the {@link WorkflowContext} class.
 *
 * <p>These tests verify that the WorkflowContext class conforms to the
 * Jakarta Agentic AI 1.0 specification requirements. This class stores
 * intermediate results and state as the workflow progresses.
 */
public class WorkflowContextTests {

    @Assertion(id = "AGENTICAI-CONTEXT-001",
               strategy = "Verify WorkflowContext class exists in the jakarta.ai.agent package")
    public void testWorkflowContextExists() {
        assertNotNull(WorkflowContext.class,
                "WorkflowContext class must exist in jakarta.ai.agent package");
    }

    @Assertion(id = "AGENTICAI-CONTEXT-002",
               strategy = "Verify WorkflowContext is a class (not interface or enum)")
    public void testWorkflowContextIsClass() {
        assertFalse(WorkflowContext.class.isInterface(),
                "WorkflowContext must not be an interface");
        assertFalse(WorkflowContext.class.isEnum(),
                "WorkflowContext must not be an enum");
        assertFalse(WorkflowContext.class.isAnnotation(),
                "WorkflowContext must not be an annotation");
    }

    @Assertion(id = "AGENTICAI-CONTEXT-003",
               strategy = "Verify WorkflowContext has setAttribute(String, Object) method")
    public void testSetAttributeMethodExists() throws NoSuchMethodException {
        Method method = WorkflowContext.class.getMethod("setAttribute", String.class, Object.class);
        assertNotNull(method,
                "setAttribute(String, Object) method must exist");
        assertTrue(Modifier.isPublic(method.getModifiers()),
                "setAttribute() must be public");
        assertEquals(void.class, method.getReturnType(),
                "setAttribute() must return void");
    }

    @Assertion(id = "AGENTICAI-CONTEXT-004",
               strategy = "Verify WorkflowContext has getAttribute(String) method")
    public void testGetAttributeMethodExists() throws NoSuchMethodException {
        Method method = WorkflowContext.class.getMethod("getAttribute", String.class);
        assertNotNull(method,
                "getAttribute(String) method must exist");
        assertTrue(Modifier.isPublic(method.getModifiers()),
                "getAttribute() must be public");
        assertEquals(Object.class, method.getReturnType(),
                "getAttribute() must return Object");
    }

    @Assertion(id = "AGENTICAI-CONTEXT-005",
               strategy = "Verify WorkflowContext has getTriggerEvent() method")
    public void testGetTriggerEventMethodExists() throws NoSuchMethodException {
        Method method = WorkflowContext.class.getMethod("getTriggerEvent");
        assertNotNull(method,
                "getTriggerEvent() method must exist");
        assertTrue(Modifier.isPublic(method.getModifiers()),
                "getTriggerEvent() must be public");
        assertEquals(Object.class, method.getReturnType(),
                "getTriggerEvent() must return Object");
    }

    @Assertion(id = "AGENTICAI-CONTEXT-006",
               strategy = "Verify WorkflowContext can be instantiated")
    public void testWorkflowContextCanBeInstantiated() {
        WorkflowContext context = new WorkflowContext();
        assertNotNull(context,
                "WorkflowContext must be instantiable");
    }

    @Assertion(id = "AGENTICAI-CONTEXT-007",
               strategy = "Verify setAttribute and getAttribute work correctly for storing values")
    public void testSetAndGetAttribute() {
        WorkflowContext context = new WorkflowContext();
        String key = "testKey";
        String value = "testValue";

        context.setAttribute(key, value);
        Object retrieved = context.getAttribute(key);

        assertEquals(value, retrieved,
                "getAttribute() must return the value set by setAttribute()");
    }

    @Assertion(id = "AGENTICAI-CONTEXT-008",
               strategy = "Verify getAttribute returns null for non-existent key")
    public void testGetAttributeReturnsNullForNonExistentKey() {
        WorkflowContext context = new WorkflowContext();

        Object result = context.getAttribute("nonExistentKey");

        assertNull(result,
                "getAttribute() must return null for non-existent key");
    }

    @Assertion(id = "AGENTICAI-CONTEXT-009",
               strategy = "Verify setAttribute can store different types of objects")
    public void testSetAttributeWithDifferentTypes() {
        WorkflowContext context = new WorkflowContext();

        // Store different types
        context.setAttribute("string", "Hello");
        context.setAttribute("integer", 42);
        context.setAttribute("boolean", true);
        context.setAttribute("object", new Object());

        // Verify retrieval
        assertEquals("Hello", context.getAttribute("string"));
        assertEquals(42, context.getAttribute("integer"));
        assertEquals(true, context.getAttribute("boolean"));
        assertNotNull(context.getAttribute("object"));
    }

    @Assertion(id = "AGENTICAI-CONTEXT-010",
               strategy = "Verify setAttribute can overwrite existing values")
    public void testSetAttributeOverwritesExistingValue() {
        WorkflowContext context = new WorkflowContext();
        String key = "key";

        context.setAttribute(key, "firstValue");
        context.setAttribute(key, "secondValue");

        assertEquals("secondValue", context.getAttribute(key),
                "setAttribute() must overwrite existing values");
    }

    @Assertion(id = "AGENTICAI-CONTEXT-011",
               strategy = "Verify getTriggerEvent returns null initially")
    public void testGetTriggerEventReturnsNullInitially() {
        WorkflowContext context = new WorkflowContext();

        Object triggerEvent = context.getTriggerEvent();

        assertNull(triggerEvent,
                "getTriggerEvent() must return null initially");
    }
}
