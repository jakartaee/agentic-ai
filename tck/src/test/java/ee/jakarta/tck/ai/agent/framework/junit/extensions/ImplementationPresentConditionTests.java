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
package ee.jakarta.tck.ai.agent.framework.junit.extensions;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the Issue 51 run-mode default.
 *
 * <p>These exercise the TCK's own infrastructure, not the specification.
 * They are tagged {@code internal} so they do not inflate spec assertion
 * counts.</p>
 */
@Tag("internal")
public class ImplementationPresentConditionTests {

    @Test
    void implementationModeIsTheDefault() {
        assertTrue(ImplementationPresentCondition.runAgainstImplementation(false),
                "an omitted baseline opt-out must run implementation assertions");
    }

    @Test
    void explicitBaselineDisablesImplementationMode() {
        assertFalse(ImplementationPresentCondition.runAgainstImplementation(true),
                "the explicit baseline opt-out must select plain-CDI assertions");
    }
}
