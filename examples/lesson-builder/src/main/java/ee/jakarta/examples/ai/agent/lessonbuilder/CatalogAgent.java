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
package ee.jakarta.examples.ai.agent.lessonbuilder;

import ee.jakarta.examples.ai.agent.lessonbuilder.model.CatalogEntry;
import ee.jakarta.examples.ai.agent.lessonbuilder.model.ContentCheck;
import ee.jakarta.examples.ai.agent.lessonbuilder.model.Coverage;
import ee.jakarta.examples.ai.agent.lessonbuilder.model.DuplicateCheck;
import ee.jakarta.examples.ai.agent.lessonbuilder.model.Estimate;
import ee.jakarta.examples.ai.agent.lessonbuilder.model.Lesson;
import ee.jakarta.examples.ai.agent.lessonbuilder.model.LessonPublished;
import ee.jakarta.examples.ai.agent.lessonbuilder.model.Standard;
import ee.jakarta.examples.ai.agent.lessonbuilder.model.Summary;
import ee.jakarta.examples.ai.agent.lessonbuilder.model.TaggedStandards;
import jakarta.ai.agent.Action;
import jakarta.ai.agent.Agent;
import jakarta.ai.agent.Decision;
import jakarta.ai.agent.LargeLanguageModel;
import jakarta.ai.agent.Outcome;
import jakarta.ai.agent.Result;
import jakarta.ai.agent.Trigger;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Files a published lesson in the course catalog. Two decisions gate the
 * work: the content must match the subject and title the teacher put on it,
 * and the catalog must not already carry the same lesson. If either fails,
 * the agent halts with a readable reason for the teacher to act on. If both
 * pass, the agent tags the lesson against the curriculum standards, estimates
 * the class time it needs, writes a short abstract, and files the entry.
 */
@Agent(name = "CatalogAgent",
        description = "Files a published lesson in the course catalog, "
                + "with coverage, curriculum standards, class-time estimate and a short abstract.")
public class CatalogAgent {

    private static final Logger LOGGER = Logger.getLogger(CatalogAgent.class.getName());

    @Inject
    Catalog catalog;

    @Inject
    StandardsReference standardsReference;

    @Inject
    CatalogOutput output;

    @Trigger
    void onPublished(LessonPublished event) {
        LOGGER.info("[TRIGGER] lesson published: " + event.lesson().getTitle());
    }

    @Action(order = 1)
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
    Result contentMatchesClaim(LessonPublished event, Coverage coverage, LargeLanguageModel model) {
        Lesson lesson = event.lesson();
        ContentCheck check = model.query(
                "A lesson has been published under subject {} and title {}. Its content covers {}."
                        + " Decide whether the content is actually about that stated subject and title,"
                        + " or whether the teacher pasted material that drifted onto a different topic."
                        + " Return ONLY JSON: {\"matches\":true|false,"
                        + "\"actualTopic\":\"short phrase naming what the content is actually about\","
                        + "\"reason\":\"one sentence\"}",
                ContentCheck.class,
                lesson.getSubject(), lesson.getTitle(), coverage.covers());
        if (!check.matches()) {
            output.setStopReason("Published, but not cataloged: the content mostly covers "
                    + check.actualTopic() + ", not " + lesson.getTitle()
                    + ". Rename the lesson or paste matching material.");
        }
        LOGGER.info("[DECISION] contentMatchesClaim: " + check.matches() + " — " + check.reason());
        return new Result(check.matches(), check);
    }

    @Decision
    Result notDuplicate(LessonPublished event, Coverage coverage, LargeLanguageModel model) {
        List<CatalogEntry> existing = catalog.entries();
        if (existing.isEmpty()) {
            LOGGER.info("[DECISION] notDuplicate: true (catalog is empty)");
            return new Result(true, null);
        }
        DuplicateCheck check = model.query(
                "Does this lesson substantially duplicate one already in the course catalog?"
                        + " It is a duplicate only if an existing entry covers the same topic at the"
                        + " same depth; a lesson that extends or revisits an existing one is not a"
                        + " duplicate. Return ONLY JSON: {\"unique\":true|false,"
                        + "\"matchedTitle\":\"title of the overlapping entry, or empty\","
                        + "\"reason\":\"one sentence\"}\n"
                        + "New lesson: {} ({}) covering {}\n"
                        + "Catalog: {}",
                DuplicateCheck.class,
                event.lesson().getTitle(), event.lesson().getSubject(), coverage.covers(), existing);
        if (!check.unique()) {
            output.setStopReason("Published, but not cataloged: " + check.matchedTitle()
                    + " already covers this. " + check.reason() + " Merge, or title this as a variant.");
        }
        LOGGER.info("[DECISION] notDuplicate: " + check.unique() + " — " + check.reason());
        return new Result(check.unique(), check);
    }

    @Action(order = 2)
    TaggedStandards tagStandards(LessonPublished event, Coverage coverage, LargeLanguageModel model) {
        LOGGER.info("[ACTION] tagStandards");
        List<Standard> candidates = standardsReference.forSubject(event.lesson().getSubject());
        if (candidates.isEmpty()) {
            return new TaggedStandards(List.of());
        }
        return model.query(
                "Which of these curriculum standards does the lesson actually meet? Only include"
                        + " a standard if the lesson's content clearly supports it. Return ONLY JSON:"
                        + " {\"codes\":[\"...\"]}\n"
                        + "Standards: {}\n"
                        + "Lesson covers: {}\n"
                        + "Lesson: {}",
                TaggedStandards.class,
                candidates, coverage.covers(), event.lesson());
    }

    @Action(order = 3)
    Estimate classify(LessonPublished event, Coverage coverage, LargeLanguageModel model) {
        LOGGER.info("[ACTION] classify");
        return model.query(
                "Estimate the minutes of class time this lesson needs and its difficulty "
                        + "(introductory, intermediate or advanced). It covers: {}\n"
                        + "Return ONLY JSON: {\"minutes\":45,\"difficulty\":\"...\"}\n{}",
                Estimate.class,
                coverage.covers(), event.lesson());
    }

    @Action(order = 4)
    Summary describe(LargeLanguageModel model) {
        LOGGER.info("[ACTION] describe");
        return new Summary(model.query(
                "Write a three-sentence catalog abstract for the lesson, in neutral prose that"
                        + " another teacher browsing the library could use to decide whether to open"
                        + " it. Name the topic, say what the lesson teaches, and say who it is for."
                        + " Return plain prose only, no quotation marks or markdown."));
    }

    @Outcome
    void file(LessonPublished event, Coverage coverage, TaggedStandards tagged,
              Estimate estimate, Summary summary) {
        Lesson lesson = event.lesson();
        CatalogEntry entry = new CatalogEntry(
                slug(lesson.getSubject(), lesson.getTitle()),
                lesson.getSubject(),
                lesson.getTitle(),
                summary.text(),
                coverage,
                estimate,
                tagged.codes());
        catalog.add(entry);
        output.setFiled(entry);
        LOGGER.info("[OUTCOME] filed: " + entry.title() + " [" + entry.id() + "]");
    }

    private static String slug(String subject, String title) {
        return (subject + "-" + title)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
    }
}
