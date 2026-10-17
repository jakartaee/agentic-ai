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

import java.util.logging.Logger;

/**
 * Turns an approved packet into the published lesson.
 */
@Agent(name = "PublishAgent",
        description = "Turns an approved packet into a published lesson.")
public class PublishAgent {

    private static final Logger LOGGER = Logger.getLogger(PublishAgent.class.getName());

    @Inject
    PublishedLessonStore published;

    @Inject
    PublishOutput output;

    @Trigger
    void onApproved(LessonApproved event) {
        LOGGER.info("[TRIGGER] approved: " + event.packet().getChapterTitle());
    }

    @Decision
    Result readyToPublish(LessonApproved event, LargeLanguageModel model) {
        Review review = model.query(
                "You are the editor reviewing this lesson packet before it is published to "
                        + "students. It is ready only if it has an introduction, a quiz and a "
                        + "conclusion, they are accurate, and they agree with each other. Return "
                        + "ONLY JSON: {\"ready\":true|false,\"reasons\":\"one or two sentences\"}\n{}",
                Review.class,
                event.packet());
        if (!review.ready()) {
            output.setStopReason("Not published — " + review.reasons());
        }
        LOGGER.info("[DECISION] readyToPublish: " + review.ready() + " — " + review.reasons());
        return new Result(review.ready(), review);
    }

    @Action
    Objectives writeObjectives(LessonApproved event, LargeLanguageModel model) {
        LOGGER.info("[ACTION] writeObjectives");
        CoursePacket packet = event.packet();
        return model.query(
                "You are a {} teacher. Write exactly three short 'What you'll learn' bullet "
                        + "points for a student, based on this introduction. Return ONLY JSON: "
                        + "{\"lines\":[\"...\",\"...\",\"...\"]}\n{}",
                Objectives.class,
                packet.getSubject(), packet.getIntro());
    }

    @Outcome
    void publish(LessonApproved event, Objectives objectives) {
        CoursePacket packet = event.packet();
        published.publish(new PublishedLesson(
                packet.getSubject(), packet.getChapterTitle(), objectives.lines(),
                packet.getIntro(), packet.getQuiz(), packet.getConclusion()));
        LOGGER.info("[OUTCOME] lesson published: " + packet.getChapterTitle());
    }
}
