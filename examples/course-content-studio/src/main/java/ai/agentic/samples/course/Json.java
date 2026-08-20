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
package ai.agentic.samples.course;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

/**
 * A tiny shared {@link Jsonb} instance. {@code Jsonb} is thread-safe and
 * expensive to build, so we keep a single application-wide instance for
 * serializing the current packet (for the refine round-trip and REST responses).
 */
public final class Json {

    private static final Jsonb JSONB = JsonbBuilder.create();

    private Json() {
    }

    public static Jsonb instance() {
        return JSONB;
    }

    /**
     * Strips markdown code fences and surrounding prose from a model response
     * and returns the outermost {@code {...}} JSON object (or {@code "{}"} if
     * none is found), so small models that wrap JSON in fences still parse.
     */
    public static String extractJson(String raw) {
        if (raw == null) {
            return "{}";
        }
        String text = raw.replace("```json", " ")
                .replace("```JSON", " ")
                .replace("```", " ")
                .trim();
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        return start >= 0 && end >= start ? text.substring(start, end + 1) : "{}";
    }
}
