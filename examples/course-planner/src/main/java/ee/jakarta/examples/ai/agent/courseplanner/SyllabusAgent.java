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
package ee.jakarta.examples.ai.agent.courseplanner;

import ee.jakarta.examples.ai.agent.courseplanner.model.Coverage;
import ee.jakarta.examples.ai.agent.courseplanner.model.Estimate;
import ee.jakarta.examples.ai.agent.courseplanner.model.Lesson;
import ee.jakarta.examples.ai.agent.courseplanner.model.LessonPublished;
import ee.jakarta.examples.ai.agent.courseplanner.model.Readiness;
import ee.jakarta.examples.ai.agent.courseplanner.model.ScheduledLesson;
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
 * Schedules a published lesson on the course syllabus, once everything it
 * assumes has been taught before it.
 */
@Agent(name = "SyllabusAgent",
        description = "Schedules a published lesson on the course syllabus once its prerequisites are taught.")
public class SyllabusAgent {

    private static final Logger LOGGER = Logger.getLogger(SyllabusAgent.class.getName());

    @Inject
    Syllabus syllabus;

    @Inject
    SyllabusOutput output;

    @Trigger
    void onPublished(LessonPublished event) {
        LOGGER.info("[TRIGGER] lesson published: " + event.lesson().getTitle());
    }

    @Action
    Coverage extractCoverage(LessonPublished event, LargeLanguageModel model) {
        LOGGER.info("[ACTION] extractCoverage");
        return model.query(
                "List what this lesson covers and what it assumes a student already knows, each "
                        + "as a short phrase. Return ONLY JSON: "
                        + "{\"covers\":[\"...\"],\"assumes\":[\"...\"]}\n{}",
                Coverage.class,
                event.lesson());
    }

    @Decision
    Result prerequisitesCovered(LessonPublished event, Coverage coverage, LargeLanguageModel model) {
        Readiness readiness = model.query(
                "A lesson is about to be scheduled on a {} course syllabus. It assumes: {}\n"
                        + "What the syllabus already covers, in week order: {}\n"
                        + "Separate what a student at this level brings from earlier schooling "
                        + "(background) from what this course itself must teach first. The course "
                        + "is ready for the lesson only if everything it must teach first is "
                        + "already on the syllabus. Return ONLY JSON: "
                        + "{\"ready\":true|false,\"missing\":[\"...\"],\"reason\":\"one sentence\"}",
                Readiness.class,
                event.lesson().getSubject(), coverage.assumes(), syllabus.lessons());
        if (!readiness.ready()) {
            output.setStopReason("Published, but not scheduled: schedule a lesson covering "
                    + String.join(" and ", readiness.missing()) + " first — " + readiness.reason());
        }
        LOGGER.info("[DECISION] prerequisitesCovered: " + readiness.ready() + " — " + readiness.reason());
        return new Result(readiness.ready(), readiness);
    }

    @Action
    Estimate estimate(LessonPublished event, Coverage coverage, LargeLanguageModel model) {
        LOGGER.info("[ACTION] estimate");
        return model.query(
                "Estimate the minutes of class time this lesson needs and its difficulty "
                        + "(introductory, intermediate or advanced). It covers: {}\n"
                        + "Return ONLY JSON: {\"minutes\":45,\"difficulty\":\"...\"}\n{}",
                Estimate.class,
                coverage.covers(), event.lesson());
    }

    @Outcome
    void schedule(LessonPublished event, Coverage coverage, Estimate estimate) {
        Lesson lesson = event.lesson();
        ScheduledLesson scheduled = new ScheduledLesson(
                syllabus.nextWeek(), lesson.getSubject(), lesson.getTitle(), coverage, estimate);
        syllabus.add(scheduled);
        output.setScheduled(scheduled);
        LOGGER.info("[OUTCOME] week " + scheduled.week() + ": " + lesson.getTitle());
    }
}
