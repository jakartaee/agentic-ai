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
package ee.jakarta.tck.ai.agent.core.agent;

import ee.jakarta.tck.ai.agent.framework.junit.anno.Assertion;
import jakarta.ai.agent.LLMException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TCK tests for the {@link LLMException} class.
 *
 * <p>These tests verify that the LLMException class conforms to the
 * Jakarta Agentic AI 1.0 specification requirements.
 */
public class LLMExceptionTests {

    @Assertion(id = "AGENTICAI-LLMEXCEPTION-001",
               strategy = "Verify LLMException class exists in the jakarta.ai.agent package")
    public void testLLMExceptionExists() {
        assertNotNull(LLMException.class,
                "LLMException class must exist in jakarta.ai.agent package");
        assertEquals("jakarta.ai.agent", LLMException.class.getPackageName(),
                "LLMException must be in jakarta.ai.agent package");
    }

    @Assertion(id = "AGENTICAI-LLMEXCEPTION-002",
               strategy = "Verify LLMException extends RuntimeException")
    public void testLLMExceptionExtendsRuntimeException() {
        assertTrue(RuntimeException.class.isAssignableFrom(LLMException.class),
                "LLMException must extend RuntimeException");
    }

    @Assertion(id = "AGENTICAI-LLMEXCEPTION-003",
               strategy = "Verify LLMException has no-arg constructor")
    public void testLLMExceptionNoArgConstructor() {
        try {
            LLMException exception = new LLMException();
            assertNotNull(exception,
                    "LLMException no-arg constructor must create instance");
            assertNull(exception.getMessage(),
                    "LLMException created with no-arg constructor should have null message");
        } catch (Exception e) {
            fail("LLMException no-arg constructor must be accessible: " + e.getMessage());
        }
    }

    @Assertion(id = "AGENTICAI-LLMEXCEPTION-004",
               strategy = "Verify LLMException has String message constructor")
    public void testLLMExceptionStringConstructor() {
        String testMessage = "Test LLM error message";
        LLMException exception = new LLMException(testMessage);
        
        assertNotNull(exception,
                "LLMException(String) constructor must create instance");
        assertEquals(testMessage, exception.getMessage(),
                "LLMException message must match constructor parameter");
    }

    @Assertion(id = "AGENTICAI-LLMEXCEPTION-005",
               strategy = "Verify LLMException has String message and Throwable cause constructor")
    public void testLLMExceptionStringThrowableConstructor() {
        String testMessage = "LLM service error";
        Throwable testCause = new RuntimeException("Network timeout");
        
        LLMException exception = new LLMException(testMessage, testCause);
        
        assertNotNull(exception,
                "LLMException(String, Throwable) constructor must create instance");
        assertEquals(testMessage, exception.getMessage(),
                "LLMException message must match constructor parameter");
        assertEquals(testCause, exception.getCause(),
                "LLMException cause must match constructor parameter");
    }

    @Assertion(id = "AGENTICAI-LLMEXCEPTION-006",
               strategy = "Verify LLMException has Throwable cause constructor")
    public void testLLMExceptionThrowableConstructor() {
        Throwable testCause = new IllegalArgumentException("Invalid parameter");
        
        LLMException exception = new LLMException(testCause);
        
        assertNotNull(exception,
                "LLMException(Throwable) constructor must create instance");
        assertEquals(testCause, exception.getCause(),
                "LLMException cause must match constructor parameter");
        assertTrue(exception.getMessage().contains("IllegalArgumentException"),
                "LLMException message should include cause information");
    }

    @Assertion(id = "AGENTICAI-LLMEXCEPTION-007",
               strategy = "Verify LLMException can be thrown and caught as RuntimeException")
    public void testLLMExceptionCanBeThrownAndCaught() {
        String testMessage = "LLM operation failed";
        
        try {
            throw new LLMException(testMessage);
        } catch (RuntimeException e) {
            assertTrue(e instanceof LLMException,
                    "LLMException must be catchable as RuntimeException");
            assertEquals(testMessage, e.getMessage(),
                    "Caught exception message must match");
        }
    }

    @Assertion(id = "AGENTICAI-LLMEXCEPTION-008",
               strategy = "Verify LLMException preserves stack trace")
    public void testLLMExceptionPreservesStackTrace() {
        LLMException exception = new LLMException("Test exception");
        
        StackTraceElement[] stackTrace = exception.getStackTrace();
        assertNotNull(stackTrace,
                "LLMException must have stack trace");
        assertTrue(stackTrace.length > 0,
                "LLMException stack trace must not be empty");
        
        // Verify this test method appears in the stack trace
        boolean foundTestMethod = false;
        for (StackTraceElement element : stackTrace) {
            if (element.getMethodName().equals("testLLMExceptionPreservesStackTrace")) {
                foundTestMethod = true;
                break;
            }
        }
        assertTrue(foundTestMethod,
                "LLMException stack trace must include originating method");
    }
}
