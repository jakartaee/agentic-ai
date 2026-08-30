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
 * Supplies the sample's form: a Customer registration form to contract Azul
 * Payara Server. Single source of truth for both the rendered form and the
 * generated tutorial.
 */
@ApplicationScoped
public class CustomerFormSpec {

    private final FormSpec spec = new FormSpec(
            "Contract Azul Payara Server",
            "Tell us about your organisation and your runtime needs; our team will get back to you.",
            List.of(
                    new FieldSpec("firstName", "First name", "text", true,
                            List.of(), "Given name of the main contact."),
                    new FieldSpec("lastName", "Last name", "text", true,
                            List.of(), "Family name of the main contact."),
                    new FieldSpec("businessEmail", "Business email", "email", true,
                            List.of(), "Work email we will use to follow up; personal inboxes are discouraged."),
                    new FieldSpec("company", "Company", "text", true,
                            List.of(), "Legal or trading name of the organisation."),
                    new FieldSpec("jobTitle", "Job title", "text", false,
                            List.of(), "Helps us route the enquiry to the right specialist."),
                    new FieldSpec("country", "Country", "select", true,
                            List.of("United States", "United Kingdom", "Germany", "Brazil",
                                    "India", "Japan", "Other"),
                            "Used for regional licensing and the correct sales contact."),
                    new FieldSpec("phone", "Phone", "tel", false,
                            List.of(), "Optional direct line for a faster conversation."),
                    new FieldSpec("message", "Requirements / How can we help?", "textarea", true,
                            List.of(), "Number of instances, environments, and any support expectations.")
            ));

    public FormSpec spec() {
        return spec;
    }
}
