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
import ee.jakarta.examples.ai.agent.courseplanner.model.ScheduledLesson;
import ee.jakarta.examples.ai.agent.courseplanner.model.Topics;
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
 * assumes has been scheduled before it.
 */
@Agent(name = "SyllabusAgent",
        description = "Schedules a published lesson on the course syllabus after its prerequisites.")
public class SyllabusAgent {

    private static final Logger LOGGER = Logger.getLogger(SyllabusAgent.class.getName());

    @Inject
    Syllabus syllabus;

    @Inject
    SyllabusOutput output;

    @Trigger
    void onPublished(LessonPublished event) {
        LOGGER.info("[TRIGGER] lesson published: " + event.lesson().getChapterTitle());
    }

    @Action
    Topics extractTopics(LessonPublished event, LargeLanguageModel model) {
        LOGGER.info("[ACTION] extractTopics");
        return model.query(
                "List the key topics this lesson teaches and the topics it assumes a student "
                        + "already knows, each as a short phrase. Return ONLY JSON: "
                        + "{\"teaches\":[\"...\"],\"assumes\":[\"...\"]}\n{}",
                Topics.class,
                event.lesson());
    }

    @Decision
    Result prerequisitesCovered(LessonPublished event, Topics topics, LargeLanguageModel model) {
        Coverage coverage = model.query(
                "A lesson is about to be scheduled on a {} course syllabus. It assumes: {}\n"
                        + "Topics already scheduled on the syllabus, in week order: {}\n"
                        + "Separate what a student at this level brings from earlier schooling "
                        + "(background) from topics this course itself must teach first. The "
                        + "lesson is covered only if every course-level prerequisite is already "
                        + "on the syllabus. Return ONLY JSON: "
                        + "{\"covered\":true|false,\"missing\":[\"...\"],\"reason\":\"one sentence\"}",
                Coverage.class,
                event.lesson().getSubject(), topics.assumes(), syllabus.lessons());
        if (!coverage.covered()) {
            output.setStopReason("Published, but not scheduled: schedule a lesson covering "
                    + String.join(" and ", coverage.missing()) + " first — " + coverage.reason());
        }
        LOGGER.info("[DECISION] prerequisitesCovered: " + coverage.covered() + " — " + coverage.reason());
        return new Result(coverage.covered(), coverage);
    }

    @Action
    Estimate estimate(LessonPublished event, Topics topics, LargeLanguageModel model) {
        LOGGER.info("[ACTION] estimate");
        return model.query(
                "Estimate the minutes of class time this lesson needs and its difficulty "
                        + "(introductory, intermediate or advanced). It teaches: {}\n"
                        + "Return ONLY JSON: {\"minutes\":45,\"difficulty\":\"...\"}\n{}",
                Estimate.class,
                topics.teaches(), event.lesson());
    }

    @Outcome
    void schedule(LessonPublished event, Topics topics, Estimate estimate) {
        Lesson lesson = event.lesson();
        ScheduledLesson scheduled = new ScheduledLesson(
                syllabus.nextWeek(), lesson.getSubject(), lesson.getChapterTitle(), topics, estimate);
        syllabus.add(scheduled);
        output.setScheduled(scheduled);
        LOGGER.info("[OUTCOME] week " + scheduled.week() + ": " + lesson.getChapterTitle());
    }
}
