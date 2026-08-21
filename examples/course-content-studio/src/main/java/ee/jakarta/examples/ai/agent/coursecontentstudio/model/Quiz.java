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

/**
 * A quiz: the typed result of an LLM query. Using a record (instead of parsing
 * a raw String) lets the agent ask for {@code Quiz.class} and receive structured
 * data straight from Jakarta JSON Binding.
 *
 * @param questions the quiz questions
 */
public record Quiz(List<QuizQuestion> questions) {
}
