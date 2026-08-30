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

import jakarta.validation.constraints.NotBlank;

/**
 * The CDI event that triggers the {@code CourseContentAgent} workflow.
 * <p>
 * The event carries the <em>mode</em>, exactly like a document-editing agent:
 * <ul>
 *   <li>{@code currentDraftJson} null/blank &rarr; <strong>generate</strong> a
 *       brand-new packet from {@code chapterBody};</li>
 *   <li>{@code currentDraftJson} present &rarr; <strong>refine</strong> the
 *       existing packet by applying {@code instruction}.</li>
 * </ul>
 * {@code section} scopes a refinement to a single part ({@code "intro"},
 * {@code "quiz"}, {@code "conclusion"}) or the whole packet ({@code "all"} /
 * null), so refining the quiz never disturbs the introduction.
 * <p>
 * {@code runId} correlates this run with the browser's live-progress SSE stream.
 *
 * @param subject          the subject area (e.g. {@code "Mathematics"}); shapes
 *                         the writing rubric
 * @param chapterTitle     the chapter title
 * @param chapterBody      the raw chapter content (used in generate mode)
 * @param instruction      the teacher's refinement instruction (refine mode)
 * @param section          which part to (re)write: {@code intro|quiz|conclusion|all}
 * @param currentDraftJson JSON of the current packet, or null to generate
 * @param runId            correlation id for the live-progress SSE stream
 */
public record CoursePacketRequest(
        @NotBlank String subject,
        String chapterTitle,
        String chapterBody,
        String instruction,
        String section,
        String currentDraftJson,
        String runId) {

    /** True when there is no existing draft, i.e. we generate from scratch. */
    public boolean isGenerate() {
        return currentDraftJson == null || currentDraftJson.isBlank();
    }

    /** True when the given section should be (re)written in this run. */
    public boolean targets(String candidate) {
        return section == null || section.isBlank()
                || section.equalsIgnoreCase("all")
                || section.equalsIgnoreCase(candidate);
    }
}
