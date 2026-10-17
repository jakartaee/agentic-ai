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

import ee.jakarta.examples.ai.agent.coursecontentstudio.model.CatalogueEntry;
import ee.jakarta.examples.ai.agent.coursecontentstudio.model.CoursePacket;
import ee.jakarta.examples.ai.agent.coursecontentstudio.model.LessonPublished;
import ee.jakarta.examples.ai.agent.coursecontentstudio.model.Novelty;
import ee.jakarta.examples.ai.agent.coursecontentstudio.model.Placement;
import ee.jakarta.examples.ai.agent.coursecontentstudio.model.Topics;
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
 * Files a published lesson in the course catalogue, unless it duplicates one
 * already there.
 */
@Agent(name = "CatalogueAgent",
        description = "Files a published lesson in the course catalogue by topic, prerequisite and placement.")
public class CatalogueAgent {

    private static final Logger LOGGER = Logger.getLogger(CatalogueAgent.class.getName());

    @Inject
    Catalogue catalogue;

    @Inject
    CatalogueOutput output;

    @Trigger
    void onPublished(LessonPublished event) {
        LOGGER.info("[TRIGGER] lesson published: " + event.lesson().getChapterTitle());
    }

    @Action
    Topics extractTopics(LessonPublished event, LargeLanguageModel model) {
        LOGGER.info("[ACTION] extractTopics");
        return model.query(
                "List the key topics this lesson teaches and the prerequisites a student needs "
                        + "before it, each as a short phrase. Return ONLY JSON: "
                        + "{\"topics\":[\"...\"],\"prerequisites\":[\"...\"]}\n{}",
                Topics.class,
                event.lesson());
    }

    @Decision
    Result novel(LessonPublished event, Topics topics, LargeLanguageModel model) {
        if (catalogue.entries().isEmpty()) {
            LOGGER.info("[DECISION] novel: proceed, catalogue is empty");
            return new Result(true, null);
        }
        Novelty novelty = model.query(
                "A new lesson is about to be filed in a course catalogue. Decide whether it "
                        + "substantially duplicates a lesson already there, meaning the same "
                        + "subject matter at the same level, not merely a related topic.\n"
                        + "New lesson title: {}\nNew lesson topics: {}\nCatalogue: {}\n"
                        + "Return ONLY JSON: {\"novel\":true|false,\"duplicateOf\":\"title or empty\","
                        + "\"reason\":\"one sentence\"}",
                Novelty.class,
                event.lesson().getChapterTitle(), topics.topics(), catalogue.entries());
        if (!novelty.novel()) {
            output.setStopReason("Published, but not catalogued: it duplicates \""
                    + novelty.duplicateOf() + "\" — " + novelty.reason());
        }
        LOGGER.info("[DECISION] novel: " + novelty.novel() + " — " + novelty.reason());
        return new Result(novelty.novel(), novelty);
    }

    @Action
    Placement place(LessonPublished event, Topics topics, LargeLanguageModel model) {
        LOGGER.info("[ACTION] place");
        return model.query(
                "Place this lesson in a curriculum: the grade level it suits, the curriculum "
                        + "strand it belongs to, the minutes a class needs for it, and its "
                        + "difficulty (introductory, intermediate or advanced). Its topics: {}\n"
                        + "Return ONLY JSON: {\"gradeLevel\":\"...\",\"strand\":\"...\","
                        + "\"minutes\":45,\"difficulty\":\"...\"}\n{}",
                Placement.class,
                topics.topics(), event.lesson());
    }

    @Outcome
    void file(LessonPublished event, Topics topics, Placement placement) {
        CoursePacket lesson = event.lesson();
        CatalogueEntry entry = new CatalogueEntry(
                lesson.getSubject(), lesson.getChapterTitle(), topics, placement);
        catalogue.add(entry);
        output.setEntry(entry);
        LOGGER.info("[OUTCOME] catalogued: " + lesson.getChapterTitle());
    }
}
