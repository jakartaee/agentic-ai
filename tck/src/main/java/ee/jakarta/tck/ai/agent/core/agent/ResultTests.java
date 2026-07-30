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
import ee.jakarta.tck.ai.agent.framework.junit.anno.Standalone;
import jakarta.ai.agent.Result;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TCK tests for the {@link Result} record.
 *
 * <p>These tests verify the runtime value semantics of the {@code Result}
 * record used as a {@link jakarta.ai.agent.Decision @Decision} return type.
 * {@link ee.jakarta.tck.ai.agent.framework.signature.SignatureTests} already
 * verifies the record's structure by reflection; these tests verify its
 * behavior: accessors, nullability of {@code details}, value-based equality,
 * and {@code toString}.
 */
@Standalone
public class ResultTests {

    @Assertion(id = "AGENTICAI-RESULT-001",
               section = "API Types / Result",
               strategy = "Verify Result exists in jakarta.ai.agent and is a record")
    public void testResultIsRecord() {
        assertNotNull(Result.class,
                "Result must exist in jakarta.ai.agent package");
        assertEquals("jakarta.ai.agent", Result.class.getPackageName(),
                "Result must be in jakarta.ai.agent package");
        assertTrue(Result.class.isRecord(),
                "Result must be a record type");
    }

    @Assertion(id = "AGENTICAI-RESULT-002",
               section = "API Types / Result",
               strategy = "Verify success() and details() accessors return the constructor arguments")
    public void testAccessorsReturnConstructorArguments() {
        Object details = new Object();
        Result result = new Result(true, details);

        assertTrue(result.success(),
                "success() must return the value passed to the canonical constructor");
        assertEquals(details, result.details(),
                "details() must return the value passed to the canonical constructor");
    }

    @Assertion(id = "AGENTICAI-RESULT-003",
               section = "API Types / Result",
               strategy = "Verify details may be null and a false success flag is preserved")
    public void testDetailsMayBeNull() {
        Result result = new Result(false, null);

        assertTrue(!result.success(),
                "success() must return false when constructed with false");
        assertNull(result.details(),
                "details() must be allowed to be null");
    }

    @Assertion(id = "AGENTICAI-RESULT-004",
               section = "API Types / Result",
               strategy = "Verify records with equal components are equal and share a hash code")
    public void testValueEquality() {
        String details = "context";
        Result a = new Result(true, details);
        Result b = new Result(true, details);

        assertEquals(a, b,
                "Results with equal components must be equal");
        assertEquals(a.hashCode(), b.hashCode(),
                "Equal Results must have equal hash codes");

        Result nullA = new Result(false, null);
        Result nullB = new Result(false, null);
        assertEquals(nullA, nullB,
                "Results with equal components (null details) must be equal");
        assertEquals(nullA.hashCode(), nullB.hashCode(),
                "Equal Results (null details) must have equal hash codes");
    }

    @Assertion(id = "AGENTICAI-RESULT-005",
               section = "API Types / Result",
               strategy = "Verify Results are unequal when the success flag or details differ")
    public void testInequalityWhenComponentsDiffer() {
        Result base = new Result(true, "context");

        assertNotEquals(base, new Result(false, "context"),
                "Results must be unequal when the success flag differs");
        assertNotEquals(base, new Result(true, "other"),
                "Results must be unequal when details differ");
        assertNotEquals(base, new Result(true, null),
                "Result with details must not equal Result with null details");
    }

    @Assertion(id = "AGENTICAI-RESULT-006",
               section = "API Types / Result",
               strategy = "Verify toString exposes the component values")
    public void testToStringExposesComponents() {
        Result result = new Result(true, "flagged");
        String text = result.toString();

        assertNotNull(text, "toString() must not be null");
        assertTrue(text.contains("true"),
                "toString() must expose the success component value");
        assertTrue(text.contains("flagged"),
                "toString() must expose the details component value");
    }
}
