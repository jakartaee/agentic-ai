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
package ee.jakarta.examples.ai.agent.tutorialgenerator;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * CDI event that triggers the {@link TutorialAgent} workflow.
 * <p>
 * When {@code currentGuidance} is {@code null}/blank the agent writes fresh
 * guidance; otherwise it revises {@code currentGuidance} according to
 * {@code instruction} (the chat refinement loop). The current guidance is
 * passed explicitly each turn so the model edits the real artifact rather
 * than relying on conversational memory alone.
 *
 * @param formSpec        the form to write guidance for
 * @param instruction     the developer's refinement request, or {@code null} on first generation
 * @param currentGuidance the guidance JSON to revise, or {@code null} on first generation
 */
public record TutorialRequest(@NotNull @Valid FormSpec formSpec,
                              String instruction,
                              String currentGuidance) {

    /** True when the request carries existing guidance to revise. */
    public boolean refine() {
        return currentGuidance != null && !currentGuidance.isBlank();
    }
}
