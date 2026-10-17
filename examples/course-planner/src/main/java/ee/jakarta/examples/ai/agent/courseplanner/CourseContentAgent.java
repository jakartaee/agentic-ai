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

import ee.jakarta.examples.ai.agent.courseplanner.model.ChapterSubmitted;
import ee.jakarta.examples.ai.agent.courseplanner.model.Lesson;
import ee.jakarta.examples.ai.agent.courseplanner.model.Quiz;
import jakarta.ai.agent.Action;
import jakarta.ai.agent.Agent;
import jakarta.ai.agent.LLMException;
import jakarta.ai.agent.LargeLanguageModel;
import jakarta.ai.agent.OnException;
import jakarta.ai.agent.Outcome;
import jakarta.ai.agent.Trigger;
import jakarta.inject.Inject;
import jakarta.validation.Valid;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Writes a chapter's introduction, quiz and conclusion.
 */
@Agent(name = "CourseContentAgent",
        description = "Writes an introduction, quiz and conclusion for a course chapter.")
public class CourseContentAgent {

    private static final Logger LOGGER = Logger.getLogger(CourseContentAgent.class.getName());

    @Inject
    LargeLanguageModel model;

    @Inject
    DraftStore drafts;

    @Trigger
    Lesson onChapter(@Valid ChapterSubmitted chapter) {
        LOGGER.info("[TRIGGER] new " + chapter.subject() + " chapter: " + chapter.chapterTitle());
        return new Lesson(chapter.subject(), chapter.chapterTitle());
    }

    @Action(order = 1)
    void writeIntro(ChapterSubmitted chapter, Lesson draft) {
        LOGGER.info("[ACTION] writeIntro");
        draft.setIntro(model.query(
                "You are a {} teacher. Write an engaging two-paragraph introduction for this "
                        + "chapter. Return plain prose only:\n{}",
                chapter.subject(), chapter.chapterBody()));
    }

    @Action(order = 2)
    void writeQuiz(ChapterSubmitted chapter, Lesson draft) {
        LOGGER.info("[ACTION] writeQuiz");
        draft.setQuiz(model.query(
                "You are a {} teacher. Create exactly 4 multiple-choice questions on this "
                        + "chapter. " + Quiz.SCHEMA + "\nChapter:\n{}",
                Quiz.class,
                chapter.subject(), chapter.chapterBody()));
    }

    @Action(order = 3)
    void writeConclusion(Lesson draft) {
        LOGGER.info("[ACTION] writeConclusion");
        draft.setConclusion(model.query(
                "Based on the chapter and the introduction and quiz you just produced, write a "
                        + "concise one-paragraph conclusion that ties them together. Return plain "
                        + "prose only."));
    }

    @Outcome
    void publish(Lesson draft) {
        drafts.publish(draft);
        LOGGER.info("[OUTCOME] draft ready for " + draft.getChapterTitle());
    }

    @OnException
    void onLlmFailure(LLMException failure, Lesson draft) {
        if (!draft.hasContent()) {
            throw failure;
        }
        LOGGER.log(Level.WARNING, "LLM call failed; keeping the sections written so far", failure);
        draft.setNotice("The model failed on one section (" + failure.getMessage()
                + "). What it had already written is kept; fill in the rest before you publish.");
    }
}
