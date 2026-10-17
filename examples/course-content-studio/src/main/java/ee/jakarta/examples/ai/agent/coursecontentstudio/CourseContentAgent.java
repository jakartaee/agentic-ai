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
import ee.jakarta.examples.ai.agent.coursecontentstudio.model.CoursePacketRequest;
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
 * Writes a chapter's introduction, quiz and conclusion, and rewrites any one
 * section on request.
 */
@Agent(name = "CourseContentAgent",
        description = "Generates and refines an intro, quiz and conclusion for a course chapter.")
public class CourseContentAgent {

    private static final Logger LOGGER = Logger.getLogger(CourseContentAgent.class.getName());

    private static final String QUIZ_SCHEMA =
            "Return ONLY valid JSON, no markdown fences. Each question has a \"type\". "
            + "Multiple-choice: {\"type\":\"multiple_choice\",\"prompt\":\"...\","
            + "\"options\":[\"a\",\"b\",\"c\",\"d\"],\"correctIndex\":0,\"explanation\":\"...\"}. "
            + "Open/subjective (student answers in free text): "
            + "{\"type\":\"open\",\"prompt\":\"...\",\"sampleAnswer\":\"a concise model answer\"}. "
            + "Wrap them as {\"questions\":[ ... ]}. Use \"multiple_choice\" unless an open "
            + "question is explicitly requested.";

    @Inject
    LargeLanguageModel model;

    @Inject
    SubjectRubric rubric;

    @Inject
    PacketStore store;

    @Inject
    ProgressTracker progress;

    private CoursePacket draft;

    @Trigger
    void onRequest(@Valid CoursePacketRequest request) {
        if (request.isGenerate()) {
            draft = new CoursePacket(request.subject(), request.chapterTitle());
            LOGGER.info("[TRIGGER] generate packet for " + request.subject()
                    + " / " + request.chapterTitle());
            progress.publish(request.runId(), "Trigger — new " + request.subject() + " chapter received");
        } else {
            draft = new CoursePacket(request.draft());
            LOGGER.info("[TRIGGER] refine section '" + request.section()
                    + "' with instruction: " + request.instruction());
            progress.publish(request.runId(), "Trigger — refine '" + request.section() + "'");
        }
    }

    @Action(order = 1)
    void writeIntro(CoursePacketRequest request) {
        if (!request.targets("intro")) {
            return;
        }
        LOGGER.info("[ACTION] writeIntro");
        progress.publish(request.runId(), "Writing the introduction…");
        if (request.isGenerate()) {
            draft.setIntro(model.query(
                    rubric.forSubject(request.subject())
                            + "\nWrite an engaging two-paragraph introduction for this chapter. "
                            + "Return plain prose only:\n{}",
                    request.chapterBody()));
        } else {
            draft.setIntro(model.query(
                    "Current introduction:\n{}\n\nApply this change and return only the "
                            + "new introduction as plain prose: {}",
                    draft.getIntro(), request.instruction()));
        }
    }

    @Action(order = 2)
    void writeQuiz(CoursePacketRequest request) {
        if (!request.targets("quiz")) {
            return;
        }
        LOGGER.info("[ACTION] writeQuiz");
        progress.publish(request.runId(), "Writing the quiz (typed JSON-B)…");
        if (request.isGenerate()) {
            draft.setQuiz(model.query(
                    rubric.forSubject(request.subject())
                            + "\nCreate exactly 4 multiple-choice questions based on this chapter. "
                            + QUIZ_SCHEMA + "\nChapter:\n{}",
                    Quiz.class,
                    request.chapterBody()));
        } else {
            draft.setQuiz(model.query(
                    "Current quiz JSON:\n{}\n\nApply this change: {}\n" + QUIZ_SCHEMA,
                    Quiz.class,
                    draft.getQuiz(), request.instruction()));
        }
    }

    @Action(order = 3)
    void writeConclusion(CoursePacketRequest request) {
        if (!request.targets("conclusion")) {
            return;
        }
        LOGGER.info("[ACTION] writeConclusion");
        progress.publish(request.runId(), "Writing the conclusion (from workflow memory)…");
        if (request.isGenerate()) {
            draft.setConclusion(model.query(
                    "Based on the chapter and the introduction and quiz you just produced, "
                            + "write a concise one-paragraph conclusion that ties them together. "
                            + "Return plain prose only. Use LaTeX for any formula "
                            + "(inline $...$, display $$...$$)."));
        } else {
            draft.setConclusion(model.query(
                    "Current conclusion:\n{}\n\nApply this change and return only the "
                            + "new conclusion as plain prose: {}",
                    draft.getConclusion(), request.instruction()));
        }
    }

    @Outcome
    void publish(CoursePacketRequest request) {
        store.publish(draft);
        LOGGER.info("[OUTCOME] packet published for " + draft.getChapterTitle());
        progress.publish(request.runId(), "Outcome — packet ready");
    }

    @OnException
    void onLlmFailure(LLMException failure, CoursePacketRequest request) {
        if (!draft.hasContent()) {
            throw failure;
        }
        LOGGER.log(Level.WARNING, "LLM call failed; keeping the sections written so far", failure);
        draft.setNotice("The model failed on one section (" + failure.getMessage()
                + "). What it had already written is kept; use Refine to retry the rest.");
        progress.publish(request.runId(), "LLM error — keeping the sections written so far");
    }
}
