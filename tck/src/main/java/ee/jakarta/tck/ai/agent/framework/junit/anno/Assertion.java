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
package ee.jakarta.tck.ai.agent.framework.junit.anno;

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a test method as an assertion test for the Jakarta Agentic AI specification.
 * Marks a test method and names the specification requirement it verifies.
 *
 * <p>The id is unique to the test, {@code AGENTICAI-<CONCERN>-NNN}, and is
 * the reference to quote in a TCK challenge. The section is the anchor of
 * the specification section that states the requirement.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Assertion(id = "AGENTICAI-ORDER-001",
 *            section = "agent-lifecycle",
 *            strategy = "Full T→D→A→O pipeline executes in strict phase order")
 * public void fullPipelineExecutesInStrictOrder() {
 *     // test implementation
 * }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Test
public @interface Assertion {

    /**
     * Identifies the specification requirement this test verifies. Several
     * tests may share one id when they verify the same requirement in
     * different ways.
     *
     * @return the assertion ID
     */
    String id();

    /**
     * The anchor of the specification section being verified, e.g.
     * "agent-lifecycle".
     * Enables direct mapping between TCK failures and the specification.
     *
     * @return the specification section reference
     */
    String section() default "";

    /**
     * A description of the test strategy used to verify this assertion.
     *
     * @return the test strategy description
     */
    String strategy() default "";
}
