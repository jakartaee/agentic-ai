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
package ee.jakarta.examples.ai.agent.coursecontentstudio;

import ee.jakarta.examples.ai.agent.coursecontentstudio.model.CoursePacket;
import ee.jakarta.examples.ai.agent.coursecontentstudio.model.LessonApproved;
import ee.jakarta.examples.ai.agent.coursecontentstudio.model.PublishedLesson;
import jakarta.ai.agent.Action;
import jakarta.ai.agent.Agent;
import jakarta.ai.agent.Decision;
import jakarta.ai.agent.HandleException;
import jakarta.ai.agent.LLMException;
import jakarta.ai.agent.LargeLanguageModel;
import jakarta.ai.agent.Outcome;
import jakarta.ai.agent.Trigger;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * The second agent in the pipeline. It is triggered by {@link LessonApproved},
 * which the REST layer fires when the teacher approves a packet — so the two
 * agents are composed purely through CDI events, with the approval acting as a
 * human-in-the-loop gate between them.
 * <p>
 * It turns the approved packet into a student-facing lesson: it generates
 * "what you'll learn" objectives with its own LLM turn (a separate,
 * per-workflow conversation from the authoring agent) and assembles the
 * published lesson.
 */
@Agent(name = "PublishAgent",
        description = "Turns an approved packet into a student-facing published lesson.")
public class PublishAgent {

    private static final Logger LOGGER = Logger.getLogger(PublishAgent.class.getName());

    @Inject
    LargeLanguageModel model;

    @Inject
    SubjectRubric rubric;

    @Inject
    PublishedLessonStore published;

    @Inject
    ProgressTracker progress;

    private PublishedLesson lesson;
    private String runId;

    @Trigger
    void onApproved(LessonApproved event) {
        runId = event.runId();
        CoursePacket packet = Json.instance().fromJson(event.packetJson(), CoursePacket.class);
        lesson = new PublishedLesson();
        lesson.setSubject(packet.getSubject());
        lesson.setChapterTitle(packet.getChapterTitle());
        lesson.setIntro(packet.getIntro());
        lesson.setQuiz(packet.getQuiz());
        lesson.setConclusion(packet.getConclusion());
        LOGGER.info("[TRIGGER] publishing approved lesson: " + packet.getChapterTitle());
        progress.publish(runId, "Publish agent — approved lesson received");
    }

    @Decision
    boolean hasApprovedContent(LessonApproved event) {
        boolean ok = lesson != null && lesson.getIntro() != null;
        LOGGER.info("[DECISION] hasApprovedContent=" + ok);
        progress.publish(runId, ok ? "Publish agent — content OK"
                : "Publish agent — nothing to publish");
        return ok;
    }

    @Action
    void writeObjectives(LessonApproved event) {
        LOGGER.info("[ACTION] writeObjectives");
        progress.publish(runId, "Publish agent — writing learning objectives…");
        String raw = model.query(
                rubric.forSubject(lesson.getSubject())
                        + "\nWrite exactly three short 'What you'll learn' bullet points for a "
                        + "student, one per line, no numbering or bullet characters, based on this "
                        + "introduction:\n{}",
                lesson.getIntro());
        lesson.setObjectives(toLines(raw));
    }

    @Outcome
    void publish(LessonApproved event) {
        lesson.setPublishedAt(Instant.now().toString());
        published.publish(lesson);
        LOGGER.info("[OUTCOME] lesson published: " + lesson.getChapterTitle());
        progress.publish(runId, "Publish agent — lesson is live");
        progress.complete(runId);
    }

    @HandleException
    void onLlmFailure(LLMException e) {
        LOGGER.warning("[HANDLE] publish LLM failure: " + e.getMessage());
        if (lesson != null) {
            if (lesson.getObjectives() == null) {
                lesson.setObjectives(List.of("Review the chapter and complete the quiz."));
            }
            lesson.setPublishedAt(Instant.now().toString());
            published.publish(lesson);
        }
        progress.publish(runId, "Publish agent — published without generated objectives");
        progress.complete(runId);
    }

    /** Splits model output into clean, non-empty lines, dropping bullet markers. */
    private static List<String> toLines(String raw) {
        if (raw == null) {
            return List.of();
        }
        return Arrays.stream(raw.split("\\r?\\n"))
                .map(String::strip)
                .filter(line -> !line.isBlank())
                .map(line -> line.replaceFirst("^[-*•\\d.\\)\\s]+", "").strip())
                .filter(line -> !line.isBlank())
                .collect(Collectors.toList());
    }
}
