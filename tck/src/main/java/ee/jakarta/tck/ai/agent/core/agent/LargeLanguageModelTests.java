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
import jakarta.ai.agent.LargeLanguageModel;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TCK tests for the {@link LargeLanguageModel} interface.
 *
 * <p>These tests verify that the LargeLanguageModel interface conforms to the
 * Jakarta Agentic AI 1.0 specification requirements. This interface provides
 * a lightweight facade for accessing LLM capabilities.
 */
public class LargeLanguageModelTests {

    @Assertion(id = "AGENTICAI-LLM-001",
               strategy = "Verify LargeLanguageModel interface exists in the jakarta.ai.agent package")
    public void testLargeLanguageModelExists() {
        assertNotNull(LargeLanguageModel.class,
                "LargeLanguageModel interface must exist in jakarta.ai.agent package");
    }

    @Assertion(id = "AGENTICAI-LLM-002",
               strategy = "Verify LargeLanguageModel is an interface")
    public void testLargeLanguageModelIsInterface() {
        assertTrue(LargeLanguageModel.class.isInterface(),
                "LargeLanguageModel must be an interface");
    }

    @Assertion(id = "AGENTICAI-LLM-003",
               strategy = "Verify LargeLanguageModel has query(String) method")
    public void testQueryStringMethodExists() throws NoSuchMethodException {
        Method method = LargeLanguageModel.class.getMethod("query", String.class);
        assertNotNull(method,
                "query(String) method must exist");
        assertTrue(Modifier.isPublic(method.getModifiers()),
                "query(String) must be public");
        assertEquals(String.class, method.getReturnType(),
                "query(String) must return String");
    }

    @Assertion(id = "AGENTICAI-LLM-004",
               strategy = "Verify LargeLanguageModel has query(String, Class) method")
    public void testQueryStringClassMethodExists() throws NoSuchMethodException {
        Method method = LargeLanguageModel.class.getMethod("query", String.class, Class.class);
        assertNotNull(method,
                "query(String, Class) method must exist");
        assertTrue(Modifier.isPublic(method.getModifiers()),
                "query(String, Class) must be public");
    }

    @Assertion(id = "AGENTICAI-LLM-005",
               strategy = "Verify LargeLanguageModel has query(String, Object...) method")
    public void testQueryStringObjectsMethodExists() throws NoSuchMethodException {
        Method method = LargeLanguageModel.class.getMethod("query", String.class, Object[].class);
        assertNotNull(method,
                "query(String, Object...) method must exist");
        assertTrue(Modifier.isPublic(method.getModifiers()),
                "query(String, Object...) must be public");
        assertEquals(String.class, method.getReturnType(),
                "query(String, Object...) must return String");
    }

    @Assertion(id = "AGENTICAI-LLM-006",
               strategy = "Verify LargeLanguageModel has query(String, Class, Object...) method")
    public void testQueryStringClassObjectsMethodExists() throws NoSuchMethodException {
        Method method = LargeLanguageModel.class.getMethod("query", String.class, Class.class, Object[].class);
        assertNotNull(method,
                "query(String, Class, Object...) method must exist");
        assertTrue(Modifier.isPublic(method.getModifiers()),
                "query(String, Class, Object...) must be public");
    }

    @Assertion(id = "AGENTICAI-LLM-007",
               strategy = "Verify LargeLanguageModel has unwrap(Class) method")
    public void testUnwrapMethodExists() throws NoSuchMethodException {
        Method method = LargeLanguageModel.class.getMethod("unwrap", Class.class);
        assertNotNull(method,
                "unwrap(Class) method must exist");
        assertTrue(Modifier.isPublic(method.getModifiers()),
                "unwrap(Class) must be public");
    }

    @Assertion(id = "AGENTICAI-LLM-008",
               strategy = "Verify LargeLanguageModel interface can be implemented")
    public void testLargeLanguageModelCanBeImplemented() {
        // Create a mock implementation to verify the interface can be implemented
        LargeLanguageModel mockImpl = new LargeLanguageModel() {
            @Override
            public String query(String prompt) {
                return "mock response";
            }

            @Override
            public <T> T query(String prompt, Class<T> resultType) {
                return null;
            }

            @Override
            public String query(String prompt, Object... inputs) {
                return "mock response with inputs";
            }

            @Override
            public <T> T query(String prompt, Class<T> resultType, Object... inputs) {
                return null;
            }

            @Override
            public <T> T unwrap(Class<T> implClass) {
                throw new IllegalArgumentException("Not supported");
            }
        };

        assertNotNull(mockImpl,
                "LargeLanguageModel must be implementable");
        assertNotNull(mockImpl.query("test"),
                "query(String) must be callable");
        assertNotNull(mockImpl.query("test", "input1", "input2"),
                "query(String, Object...) must be callable");
    }
}
