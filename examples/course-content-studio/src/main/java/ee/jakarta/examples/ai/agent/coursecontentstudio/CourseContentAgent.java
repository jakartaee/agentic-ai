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

import ee.jakarta.examples.ai.agent.coursecontentstudio.model.ChapterSubmitted;
import ee.jakarta.examples.ai.agent.coursecontentstudio.model.CoursePacket;
import ee.jakarta.examples.ai.agent.coursecontentstudio.model.Quiz;
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
    SubjectRubric rubric;

    @Inject
    PacketStore store;

    @Inject
    ProgressTracker progress;

    @Trigger
    CoursePacket onChapter(@Valid ChapterSubmitted chapter) {
        LOGGER.info("[TRIGGER] new " + chapter.subject() + " chapter: " + chapter.chapterTitle());
        progress.publish(chapter.runId(), "Trigger — new " + chapter.subject() + " chapter received");
        return new CoursePacket(chapter.subject(), chapter.chapterTitle());
    }

    @Action(order = 1)
    void writeIntro(ChapterSubmitted chapter, CoursePacket draft) {
        LOGGER.info("[ACTION] writeIntro");
        progress.publish(chapter.runId(), "Writing the introduction…");
        draft.setIntro(model.query(
                rubric.forSubject(chapter.subject())
                        + "\nWrite an engaging two-paragraph introduction for this chapter. "
                        + "Return plain prose only:\n{}",
                chapter.chapterBody()));
    }

    @Action(order = 2)
    void writeQuiz(ChapterSubmitted chapter, CoursePacket draft) {
        LOGGER.info("[ACTION] writeQuiz");
        progress.publish(chapter.runId(), "Writing the quiz…");
        draft.setQuiz(model.query(
                rubric.forSubject(chapter.subject())
                        + "\nCreate exactly 4 multiple-choice questions based on this chapter. "
                        + Quiz.SCHEMA + "\nChapter:\n{}",
                Quiz.class,
                chapter.chapterBody()));
    }

    @Action(order = 3)
    void writeConclusion(ChapterSubmitted chapter, CoursePacket draft) {
        LOGGER.info("[ACTION] writeConclusion");
        progress.publish(chapter.runId(), "Writing the conclusion (from workflow memory)…");
        draft.setConclusion(model.query(
                "Based on the chapter and the introduction and quiz you just produced, "
                        + "write a concise one-paragraph conclusion that ties them together. "
                        + "Return plain prose only. Use LaTeX for any formula "
                        + "(inline $...$, display $$...$$)."));
    }

    @Outcome
    void publish(ChapterSubmitted chapter, CoursePacket draft) {
        store.publish(draft);
        LOGGER.info("[OUTCOME] packet published for " + draft.getChapterTitle());
        progress.publish(chapter.runId(), "Outcome — packet ready");
    }

    @OnException
    void onLlmFailure(LLMException failure, ChapterSubmitted chapter, CoursePacket draft) {
        if (!draft.hasContent()) {
            throw failure;
        }
        LOGGER.log(Level.WARNING, "LLM call failed; keeping the sections written so far", failure);
        draft.setNotice("The model failed on one section (" + failure.getMessage()
                + "). What it had already written is kept; use Refine to write the rest.");
        progress.publish(chapter.runId(), "LLM error — keeping the sections written so far");
    }
}
