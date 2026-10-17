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
import ee.jakarta.examples.ai.agent.coursecontentstudio.model.Objectives;
import ee.jakarta.examples.ai.agent.coursecontentstudio.model.PublishedLesson;
import jakarta.ai.agent.Action;
import jakarta.ai.agent.Agent;
import jakarta.ai.agent.LargeLanguageModel;
import jakarta.ai.agent.Outcome;
import jakarta.ai.agent.Trigger;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.logging.Logger;

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

    @Trigger
    void onApproved(LessonApproved event) {
        LOGGER.info("[TRIGGER] publishing approved lesson: " + event.packet().getChapterTitle());
        progress.publish(event.runId(), "Publish agent — approved lesson received");
    }

    @Action
    Objectives writeObjectives(LessonApproved event) {
        LOGGER.info("[ACTION] writeObjectives");
        progress.publish(event.runId(), "Publish agent — writing learning objectives…");
        CoursePacket packet = event.packet();
        return model.query(
                rubric.forSubject(packet.getSubject())
                        + "\nWrite exactly three short 'What you'll learn' bullet points for a "
                        + "student, based on this introduction. Return ONLY JSON: "
                        + "{\"lines\":[\"...\",\"...\",\"...\"]}\n{}",
                Objectives.class,
                packet.getIntro());
    }

    @Outcome
    void publish(LessonApproved event, Objectives objectives) {
        CoursePacket packet = event.packet();
        published.publish(new PublishedLesson(
                packet.getSubject(), packet.getChapterTitle(), objectives.lines(),
                packet.getIntro(), packet.getQuiz(), packet.getConclusion(),
                Instant.now().toString()));
        LOGGER.info("[OUTCOME] lesson published: " + packet.getChapterTitle());
        progress.publish(event.runId(), "Publish agent — lesson is live");
    }
}
