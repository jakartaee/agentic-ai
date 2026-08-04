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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link ImplementationPresentCondition#checkMode(String, boolean)}.
 *
 * <p>These exercise the TCK's own infrastructure, not the specification.
 * They are tagged {@code internal} so they do not inflate spec assertion
 * counts when running with {@code -Dgroups=standalone}.</p>
 */
@Tag("internal")
public class ImplementationPresentConditionTests {

    @Test
    void unsetModeIsNoOp() {
        assertDoesNotThrow(() -> ImplementationPresentCondition.checkMode(null, false));
        assertDoesNotThrow(() -> ImplementationPresentCondition.checkMode(null, true));
        assertDoesNotThrow(() -> ImplementationPresentCondition.checkMode("", false));
        assertDoesNotThrow(() -> ImplementationPresentCondition.checkMode("   ", true));
    }

    @Test
    void implementationWithoutPresentThrows() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> ImplementationPresentCondition.checkMode("implementation", false));
        assertTrue(ex.getMessage().contains(ImplementationPresentCondition.IMPLEMENTATION_PRESENT_PROPERTY),
                "Message must name the presence property");
    }

    @Test
    void baselineWithPresentThrows() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> ImplementationPresentCondition.checkMode("baseline", true));
        assertTrue(ex.getMessage().contains(ImplementationPresentCondition.IMPLEMENTATION_PRESENT_PROPERTY),
                "Message must name the presence property");
    }

    @Test
    void consistentModesDoNotThrow() {
        assertDoesNotThrow(() -> ImplementationPresentCondition.checkMode("implementation", true));
        assertDoesNotThrow(() -> ImplementationPresentCondition.checkMode("baseline", false));
    }

    @Test
    void unknownValueThrows() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> ImplementationPresentCondition.checkMode("prod", false));
        assertTrue(ex.getMessage().contains(ImplementationPresentCondition.TCK_MODE_PROPERTY),
                "Message must name the mode property");
        assertTrue(ex.getMessage().contains("prod"),
                "Message must include the offending value");
    }

    @Test
    void modeIsCaseInsensitiveAndTrimmed() {
        assertDoesNotThrow(() -> ImplementationPresentCondition.checkMode(" Implementation ", true));
        assertDoesNotThrow(() -> ImplementationPresentCondition.checkMode("BASELINE", false));
        assertDoesNotThrow(() -> ImplementationPresentCondition.checkMode("\tBaSeLiNe\n", false));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> ImplementationPresentCondition.checkMode(" IMPLEMENTATION ", false));
        assertTrue(ex.getMessage().contains(ImplementationPresentCondition.IMPLEMENTATION_PRESENT_PROPERTY));
    }
}
