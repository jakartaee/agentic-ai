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
package ee.jakarta.examples.ai.agent.coursecontentstudio.model;

import java.util.List;

public record Quiz(List<QuizQuestion> questions) {

    /** The JSON shape a model reply must have to bind to this record. */
    public static final String SCHEMA =
            "Return ONLY valid JSON, no markdown fences. Each question has a \"type\". "
            + "Multiple-choice: {\"type\":\"multiple_choice\",\"prompt\":\"...\","
            + "\"options\":[\"a\",\"b\",\"c\",\"d\"],\"correctIndex\":0,\"explanation\":\"...\"}. "
            + "Open/subjective (student answers in free text): "
            + "{\"type\":\"open\",\"prompt\":\"...\",\"sampleAnswer\":\"a concise model answer\"}. "
            + "Wrap them as {\"questions\":[ ... ]}. Use \"multiple_choice\" unless an open "
            + "question is explicitly requested.";
}
