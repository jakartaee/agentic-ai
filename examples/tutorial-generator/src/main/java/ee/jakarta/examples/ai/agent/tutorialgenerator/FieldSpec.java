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

import java.util.List;

/**
 * Metadata for one form field. This is the structured input the agent uses to
 * explain the field &mdash; the page also renders the live form from it, so the
 * form and its tutorial share a single source of truth.
 *
 * @param name     HTML field name
 * @param label    human-readable label
 * @param type     input type: text, email, tel, select, textarea, checkbox
 * @param required whether the field is mandatory
 * @param options  select options (empty for non-select fields)
 * @param help     short author hint about the field's purpose
 */
public record FieldSpec(String name, String label, String type, boolean required,
                        List<String> options, String help) {
}
