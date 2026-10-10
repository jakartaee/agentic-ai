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
package ee.jakarta.examples.ai.agent.inquiryintake;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * CDI event that triggers the {@link IntakeAgent} workflow.
 * <p>
 * On a first draft the request carries only {@code brief} (the raw customer
 * message) and the agent produces the full triaged inquiry from scratch. On
 * refine, the request carries {@code currentInquiry} plus a chat
 * {@code instruction} and the agent edits the existing record. The current
 * inquiry is passed explicitly on every refine turn so the model edits the
 * real artifact rather than relying on conversational memory alone.
 *
 * @param formSpec       the schema the inquiry must match
 * @param brief          the raw customer message, or {@code null} on refine
 * @param instruction    the chat refinement instruction, or {@code null} on first draft
 * @param currentInquiry the inquiry JSON to revise, or {@code null} on first draft
 */
public record IntakeRequest(@NotNull @Valid FormSpec formSpec,
                            String brief,
                            String instruction,
                            String currentInquiry) {

    /** True when the request carries an existing inquiry to revise. */
    public boolean refine() {
        return currentInquiry != null && !currentInquiry.isBlank();
    }
}
