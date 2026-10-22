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
 ****************************************************************************/
package ee.jakarta.examples.ai.agent.lessonbuilder.model;

/**
 * The model's verdict on whether the lesson substantially duplicates one
 * already in the catalog: if not unique, which entry overlaps and why.
 */
public record DuplicateCheck(boolean unique, String matchedTitle, String reason) {

    /** The JSON shape a model reply must have to bind to this record. */
    public static final String SCHEMA =
            "Return ONLY valid JSON of the form {\"unique\":true|false,"
            + "\"matchedTitle\":\"title of the overlapping entry, or empty\","
            + "\"reason\":\"one sentence\"}.";
}
