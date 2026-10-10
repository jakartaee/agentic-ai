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

import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * The customer-facing contact form: a few identity fields, a routing
 * category, and a free-text message. Single source of truth for the rendered
 * form and for the AI assists that pre-fill or refine individual fields.
 */
@ApplicationScoped
public class InquiryFormSpec {

    public static final List<String> INQUIRY_TYPES =
            List.of("General", "Sales", "Support", "Partnership");

    private final FormSpec spec = new FormSpec(
            "Contact us",
            "Tell us a little about yourself and what you need. We will get back to you shortly.",
            List.of(
                    new FieldSpec("name", "Name", "text",
                            List.of(),
                            "Your name as you'd like us to address you."),
                    new FieldSpec("businessEmail", "Business email", "email",
                            List.of(),
                            "The address we should reply to."),
                    new FieldSpec("company", "Company", "text",
                            List.of(),
                            "The organization you are reaching out on behalf of."),
                    new FieldSpec("inquiryType", "What can we help with?", "select",
                            INQUIRY_TYPES,
                            "Pick the closest fit, or let the assistant suggest one from your message."),
                    new FieldSpec("message", "How can we help?", "textarea",
                            List.of(),
                            "Describe what you are looking for. The assistant can draft or refine this for you.")
            ));

    public FormSpec spec() {
        return spec;
    }
}
