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
import ee.jakarta.examples.ai.agent.coursecontentstudio.model.Review;
import jakarta.ai.agent.Action;
import jakarta.ai.agent.Agent;
import jakarta.ai.agent.Decision;
import jakarta.ai.agent.LargeLanguageModel;
import jakarta.ai.agent.Outcome;
import jakarta.ai.agent.Result;
import jakarta.ai.agent.Trigger;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.logging.Logger;

/**
 * Turns an approved packet into the student-facing lesson.
 */
@Agent(name = "PublishAgent",
        description = "Turns an approved packet into a student-facing published lesson.")
public class PublishAgent {

    private static final Logger LOGGER = Logger.getLogger(PublishAgent.class.getName());

    @Inject
    SubjectRubric rubric;

    @Inject
    PublishedLessonStore published;

    @Inject
    PublishOutput output;

    @Inject
    ProgressTracker progress;

    @Trigger
    void onApproved(LessonApproved event) {
        LOGGER.info("[TRIGGER] publishing approved lesson: " + event.packet().getChapterTitle());
        progress.publish(event.runId(), "Publish agent — approved lesson received");
    }

    @Decision
    Result readyToPublish(LessonApproved event, LargeLanguageModel model) {
        LOGGER.info("[DECISION] readyToPublish");
        progress.publish(event.runId(), "Publish agent — reviewing the packet…");
        CoursePacket packet = event.packet();
        Review review = model.query(
                rubric.forSubject(packet.getSubject())
                        + "\nYou are the editor reviewing this lesson packet before it is published "
                        + "to students. It is ready only if it has an introduction, a quiz and a "
                        + "conclusion, they are accurate, and they agree with each other. Return "
                        + "ONLY JSON: {\"ready\":true|false,\"reasons\":\"one or two sentences\"}\n{}",
                Review.class,
                packet);
        if (!review.ready()) {
            output.setStopReason("Not published — " + review.reasons());
        }
        LOGGER.info("[DECISION] readyToPublish: " + review.ready() + " — " + review.reasons());
        progress.publish(event.runId(), review.ready()
                ? "Publish agent — review passed"
                : "Publish agent — review failed, not published");
        return new Result(review.ready(), review);
    }

    @Action
    Objectives writeObjectives(LessonApproved event, LargeLanguageModel model) {
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
