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

import ee.jakarta.examples.ai.agent.courseplanner.model.Lesson;
import ee.jakarta.examples.ai.agent.courseplanner.model.Quiz;
import ee.jakarta.examples.ai.agent.courseplanner.model.Topic;
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
 * Writes a lesson's introduction, quiz and conclusion from a topic and its
 * material.
 */
@Agent(name = "LessonAgent",
        description = "Writes a lesson's introduction, quiz and conclusion from a topic and its material.")
public class LessonAgent {

    private static final Logger LOGGER = Logger.getLogger(LessonAgent.class.getName());

    @Inject
    LargeLanguageModel model;

    @Inject
    DraftStore drafts;

    @Trigger
    Lesson onTopic(@Valid Topic topic) {
        LOGGER.info("[TRIGGER] new " + topic.subject() + " topic: " + topic.title());
        return new Lesson(topic.subject(), topic.title());
    }

    @Action(order = 1)
    void writeIntro(Topic topic, Lesson draft) {
        LOGGER.info("[ACTION] writeIntro");
        draft.setIntro(model.query(
                "You are a {} teacher. Write an engaging two-paragraph introduction for a lesson "
                        + "on {}, based on this material. Return plain prose only:\n{}",
                topic.subject(), topic.title(), topic.material()));
    }

    @Action(order = 2)
    void writeQuiz(Topic topic, Lesson draft) {
        LOGGER.info("[ACTION] writeQuiz");
        draft.setQuiz(model.query(
                "You are a {} teacher. Create exactly 4 multiple-choice questions for a lesson "
                        + "on {}, based on this material. " + Quiz.SCHEMA + "\nMaterial:\n{}",
                Quiz.class,
                topic.subject(), topic.title(), topic.material()));
    }

    @Action(order = 3)
    void writeConclusion(Lesson draft) {
        LOGGER.info("[ACTION] writeConclusion");
        draft.setConclusion(model.query(
                "Based on the material and the introduction and quiz you just produced, write a "
                        + "concise one-paragraph conclusion that ties them together. Return plain "
                        + "prose only."));
    }

    @Outcome
    void storeDraft(Lesson draft) {
        drafts.set(draft);
        LOGGER.info("[OUTCOME] draft ready for " + draft.getTitle());
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
