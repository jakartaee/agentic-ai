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

import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * Supplies the sample's form: a generic customer onboarding form. Single
 * source of truth for both the rendered form and the generated guidance.
 */
@ApplicationScoped
public class CustomerFormSpec {

    private final FormSpec spec = new FormSpec(
            "Customer onboarding",
            "Tell us about your organization. Our team will follow up shortly.",
            List.of(
                    new FieldSpec("firstName", "First name", "text", true,
                            List.of(), "First name of the main contact."),
                    new FieldSpec("lastName", "Last name", "text", true,
                            List.of(), "Last name of the main contact."),
                    new FieldSpec("businessEmail", "Business email", "email", true,
                            List.of(), "Work email we should use for follow-up. Personal inboxes are discouraged."),
                    new FieldSpec("company", "Company", "text", true,
                            List.of(), "Legal or trading name of the organization."),
                    new FieldSpec("jobTitle", "Job title", "text", false,
                            List.of(), "Helps us route your request to the right specialist."),
                    new FieldSpec("country", "Country", "select", true,
                            List.of("United States", "United Kingdom", "Germany", "Brazil",
                                    "India", "Japan", "Other"),
                            "Used to route your request to the right regional team."),
                    new FieldSpec("phone", "Phone", "tel", false,
                            List.of(), "Optional direct line if you'd prefer a faster follow-up."),
                    new FieldSpec("message", "How can we help?", "textarea", true,
                            List.of(), "A brief description of what you're looking for. The more specific, the better.")
            ));

    public FormSpec spec() {
        return spec;
    }
}
