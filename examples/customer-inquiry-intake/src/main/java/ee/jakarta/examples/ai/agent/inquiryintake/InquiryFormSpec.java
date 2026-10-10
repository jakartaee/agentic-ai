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
 * The structured shape of a triaged customer inquiry: a one-line summary, a
 * routing category, an urgency, a cleaned-up message, and a suggested next
 * step. Single source of truth for both the agent prompt and the page UI.
 */
@ApplicationScoped
public class InquiryFormSpec {

    private final FormSpec spec = new FormSpec(
            "Triaged inquiry",
            "The structured record the triage agent produces from the raw inquiry above.",
            List.of(
                    new FieldSpec("summary", "Summary", "text",
                            List.of(),
                            "One line a teammate can scan in a queue."),
                    new FieldSpec("category", "Category", "select",
                            List.of("Sales", "Support", "Technical", "Billing"),
                            "Which team should pick this up."),
                    new FieldSpec("urgency", "Urgency", "select",
                            List.of("Low", "Medium", "High"),
                            "How time-sensitive the request is."),
                    new FieldSpec("message", "Cleaned-up message", "textarea",
                            List.of(),
                            "The customer's own words, lightly edited for clarity."),
                    new FieldSpec("suggestedAction", "Suggested next step", "textarea",
                            List.of(),
                            "What the teammate who picks this up should do first.")
            ));

    public FormSpec spec() {
        return spec;
    }
}
