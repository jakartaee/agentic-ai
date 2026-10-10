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

/**
 * CDI event that triggers the {@link IntakeAgent} pipeline. The raw
 * submission from the contact form: whatever identity fields the customer
 * filled in, plus the free-text message that is always required.
 *
 * @param name    sender's name, or {@code null}/blank if the customer did not fill it in
 * @param email   business email, or {@code null}/blank
 * @param company organisation, or {@code null}/blank
 * @param message the customer's message (required)
 */
public record IntakeRequest(String name, String email, String company, String message) {
}
