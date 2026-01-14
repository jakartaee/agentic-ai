/*
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package ee.jakarta.tck.ai.agent.framework.junit.anno;

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a test method as an assertion test for the Jakarta Agentic AI specification.
 * This annotation maps the test to a specific specification requirement.
 *
 * <p>Each assertion test should verify a specific behavior defined in the
 * Jakarta Agentic AI 1.0 specification.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Assertion(id = "AGENTICAI-001",
 *            strategy = "Verify @Agent annotation is retained at runtime")
 * public void testAgentAnnotationRetention() {
 *     // test implementation
 * }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Test
public @interface Assertion {

    /**
     * The unique identifier for this assertion.
     * This should correspond to a requirement in the specification.
     *
     * @return the assertion ID
     */
    String id();

    /**
     * A description of the test strategy used to verify this assertion.
     *
     * @return the test strategy description
     */
    String strategy() default "";
}
