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
 * The student-facing lesson produced by {@code PublishAgent} from an approved
 * packet: the same intro/quiz/conclusion, plus generated "what you'll learn"
 * objectives and a publication timestamp.
 */
public record PublishedLesson(
        String subject,
        String chapterTitle,
        List<String> objectives,
        String intro,
        Quiz quiz,
        String conclusion,
        String publishedAt) {
}
