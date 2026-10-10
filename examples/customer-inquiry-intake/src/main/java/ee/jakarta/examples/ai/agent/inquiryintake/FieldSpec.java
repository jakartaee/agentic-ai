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

import java.util.List;

/**
 * Metadata for one field of a structured customer inquiry. The agent fills
 * this field from the raw brief; the page also renders the editable control
 * from it, so the schema and the UI share a single source of truth.
 *
 * @param name     field name used as the JSON key in the inquiry record
 * @param label    human-readable label
 * @param type     input type: text, select, textarea
 * @param options  select options (empty for non-select fields)
 * @param help     short hint shown under the control
 */
public record FieldSpec(String name, String label, String type,
                        List<String> options, String help) {
}
