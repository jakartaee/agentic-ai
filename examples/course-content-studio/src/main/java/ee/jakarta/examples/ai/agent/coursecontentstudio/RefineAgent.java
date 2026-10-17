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
import ee.jakarta.examples.ai.agent.coursecontentstudio.model.Quiz;
import ee.jakarta.examples.ai.agent.coursecontentstudio.model.SectionRefineRequested;
import jakarta.ai.agent.Action;
import jakarta.ai.agent.Agent;
import jakarta.ai.agent.LargeLanguageModel;
import jakarta.ai.agent.Outcome;
import jakarta.ai.agent.Trigger;
import jakarta.inject.Inject;
import jakarta.validation.Valid;

import java.util.Locale;
import java.util.logging.Logger;

/**
 * Rewrites one section of the current packet from the teacher's instruction.
 */
@Agent(name = "RefineAgent",
        description = "Rewrites one section of a course packet from the teacher's instruction.")
public class RefineAgent {

    private static final Logger LOGGER = Logger.getLogger(RefineAgent.class.getName());

    @Inject
    LargeLanguageModel model;

    @Inject
    PacketStore store;

    @Inject
    ProgressTracker progress;

    @Trigger
    void onRefine(@Valid SectionRefineRequested request) {
        LOGGER.info("[TRIGGER] refine " + request.section() + ": " + request.instruction());
        progress.publish(request.runId(), "Trigger — refine the " + name(request));
    }

    @Action
    CoursePacket rewrite(SectionRefineRequested request) {
        LOGGER.info("[ACTION] rewrite " + request.section());
        progress.publish(request.runId(), "Rewriting the " + name(request) + "…");
        CoursePacket updated = new CoursePacket(request.draft());
        switch (request.section()) {
            case INTRO -> updated.setIntro(model.query(
                    "Current introduction:\n{}\n\nApply this change and return only the "
                            + "new introduction as plain prose: {}",
                    updated.getIntro(), request.instruction()));
            case QUIZ -> updated.setQuiz(model.query(
                    "Current quiz JSON:\n{}\n\nApply this change: {}\n" + Quiz.SCHEMA,
                    Quiz.class,
                    updated.getQuiz(), request.instruction()));
            case CONCLUSION -> updated.setConclusion(model.query(
                    "Current conclusion:\n{}\n\nApply this change and return only the "
                            + "new conclusion as plain prose: {}",
                    updated.getConclusion(), request.instruction()));
        }
        return updated;
    }

    @Outcome
    void publish(SectionRefineRequested request, CoursePacket updated) {
        store.publish(updated);
        LOGGER.info("[OUTCOME] " + request.section() + " rewritten for " + updated.getChapterTitle());
        progress.publish(request.runId(), "Outcome — packet updated");
    }

    private static String name(SectionRefineRequested request) {
        return request.section().name().toLowerCase(Locale.ROOT);
    }
}
