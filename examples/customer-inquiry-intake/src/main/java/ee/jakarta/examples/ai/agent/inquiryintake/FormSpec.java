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
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Schema for a structured customer inquiry: a title, a short intro, and the
 * fields the agent will fill.
 *
 * @param title  schema title
 * @param intro  short description shown above the form
 * @param fields the inquiry fields
 */
public record FormSpec(String title, String intro, @NotEmpty List<@Valid FieldSpec> fields) {
}
