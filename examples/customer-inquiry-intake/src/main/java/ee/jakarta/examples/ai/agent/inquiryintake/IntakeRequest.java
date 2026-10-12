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

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * CDI event that triggers the {@link IntakeAgent} pipeline. The raw
 * submission from the contact form: whatever identity fields the customer
 * filled in (all optional), plus the free-text message that is always
 * required.
 */
public record IntakeRequest(
        @Size(max = 100) String name,
        @Email @Size(max = 150) String email,
        @NotBlank @Size(max = 5000) String message) {
}
