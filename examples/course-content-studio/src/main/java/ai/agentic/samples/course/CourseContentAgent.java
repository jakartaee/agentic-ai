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
package ai.agentic.samples.course;

import ai.agentic.samples.course.model.CoursePacket;
import ai.agentic.samples.course.model.CoursePacketRequest;
import ai.agentic.samples.course.model.Quiz;
import ai.agentic.samples.course.model.QuizQuestion;
import jakarta.ai.agent.Action;
import jakarta.ai.agent.Agent;
import jakarta.ai.agent.Decision;
import jakarta.ai.agent.HandleException;
import jakarta.ai.agent.LLMException;
import jakarta.ai.agent.LargeLanguageModel;
import jakarta.ai.agent.Outcome;
import jakarta.ai.agent.Trigger;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Generates a chapter's introduction, quiz and conclusion, and refines them on
 * the teacher's request.
 * <p>
 * No scope annotation is present, so the runtime applies {@code @WorkflowScoped}
 * (the specification default). That is what makes the {@link #draft} and
 * {@link #runId} instance fields safe as per-workflow state: one agent instance
 * exists per workflow execution, so the ordered actions build the packet up
 * incrementally without leaking across concurrent requests.
 * <p>
 * Features on show:
 * <ul>
 *   <li><b>Ordered phases</b> — {@code @Decision}/{@code @Action} carry an
 *       explicit {@code order}, so intro &rarr; quiz &rarr; conclusion run in a
 *       guaranteed sequence (all phases must be ordered once any is).</li>
 *   <li><b>Typed result via JSON-B</b> — the quiz is deserialized into a
 *       {@link Quiz} record. Small local models often wrap JSON in code fences,
 *       so we parse defensively rather than call the facade's typed overload
 *       (which would throw on the fences).</li>
 *   <li><b>Per-workflow conversational memory</b> — the conclusion query does
 *       not re-send the chapter; it relies on the earlier turns of the same
 *       workflow.</li>
 *   <li><b>Human-in-the-loop</b> — the event carries the mode (generate vs.
 *       refine) and a target section, so the teacher can refine one part.</li>
 *   <li><b>Resilience</b> — a placeholder quiz keeps the workflow moving when the
 *       model returns unusable JSON; {@code @HandleException} covers LLM outages
 *       and invalid requests.</li>
 *   <li><b>Live progress</b> — each phase reports to {@link ProgressTracker} so
 *       the browser popup shows what the agent is doing.</li>
 * </ul>
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

    /** Per-workflow state. */
    private CoursePacket draft;
    private String runId;

    @Trigger
    void onRequest(@Valid CoursePacketRequest request) {
        runId = request.runId();
        if (request.isGenerate()) {
            draft = new CoursePacket(request.subject(), request.chapterTitle());
            LOGGER.info("[TRIGGER] generate packet for " + request.subject()
                    + " / " + request.chapterTitle());
            progress.publish(runId, "Trigger — new " + request.subject() + " chapter received");
        } else {
            draft = Json.instance().fromJson(request.currentDraftJson(), CoursePacket.class);
            LOGGER.info("[TRIGGER] refine section '" + request.section()
                    + "' with instruction: " + request.instruction());
            progress.publish(runId, "Trigger — refine '" + request.section() + "'");
        }
    }

    @Decision(order = 1)
    boolean hasTeachableContent(CoursePacketRequest request) {
        boolean ok = request.isGenerate()
                ? request.chapterBody() != null && request.chapterBody().strip().length() >= 80
                : draft != null;
        LOGGER.info("[DECISION] hasTeachableContent=" + ok);
        if (ok) {
            progress.publish(runId, "Decision — content looks teachable");
        } else {
            progress.publish(runId, "Decision — not enough content, stopping");
            progress.complete(runId);
        }
        return ok;
    }

    @Action(order = 2)
    void writeIntro(CoursePacketRequest request) {
        if (!request.targets("intro")) {
            return;
        }
        LOGGER.info("[ACTION] writeIntro");
        progress.publish(runId, "Writing the introduction…");
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

    @Action(order = 3)
    void writeQuiz(CoursePacketRequest request) {
        if (!request.targets("quiz")) {
            return;
        }
        LOGGER.info("[ACTION] writeQuiz");
        progress.publish(runId, "Writing the quiz (typed JSON-B)…");
        String raw;
        if (request.isGenerate()) {
            raw = model.query(
                    rubric.forSubject(request.subject())
                            + "\nCreate exactly 4 multiple-choice questions based on this chapter. "
                            + QUIZ_SCHEMA + "\nChapter:\n{}",
                    request.chapterBody());
        } else {
            raw = model.query(
                    "Current quiz JSON:\n{}\n\nApply this change: {}\n" + QUIZ_SCHEMA,
                    draft.getQuiz(), request.instruction());
        }
        draft.setQuiz(parseQuiz(raw));
    }

    @Action(order = 4)
    void writeConclusion(CoursePacketRequest request) {
        if (!request.targets("conclusion")) {
            return;
        }
        LOGGER.info("[ACTION] writeConclusion (uses workflow memory)");
        progress.publish(runId, "Writing the conclusion (from workflow memory)…");
        if (request.isGenerate()) {
            // No chapter re-sent: relies on the intro and quiz produced earlier
            // in this same workflow (per-workflow conversational state).
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
        progress.publish(runId, "Outcome — packet ready");
        progress.complete(runId);
    }

    @HandleException
    void onLlmFailure(LLMException e) {
        LOGGER.warning("[HANDLE] LLM failure: " + e.getMessage());
        progress.publish(runId, "LLM error — publishing what we have");
        if (draft != null) {
            store.publish(draft);
        }
        progress.complete(runId);
    }

    @HandleException
    void onInvalidRequest(ConstraintViolationException e) {
        LOGGER.warning("[HANDLE] invalid request rejected: " + e.getMessage());
        progress.publish(runId, "Invalid request rejected");
        progress.complete(runId);
    }

    /**
     * Deserializes the quiz defensively: strips any markdown code fences and
     * surrounding prose, extracts the JSON object, and binds it to {@link Quiz}
     * with Jakarta JSON Binding. Falls back to a placeholder so a bad model
     * response never aborts the workflow.
     */
    private Quiz parseQuiz(String raw) {
        try {
            Quiz quiz = Json.instance().fromJson(Json.extractJson(raw), Quiz.class);
            if (quiz != null && quiz.questions() != null && !quiz.questions().isEmpty()) {
                return normalize(quiz);
            }
        } catch (RuntimeException parseFailed) {
            LOGGER.warning("[ACTION] quiz JSON parse failed: " + parseFailed.getMessage());
        }
        LOGGER.warning("[ACTION] quiz JSON unusable; using placeholder");
        progress.publish(runId, "Quiz JSON was unusable — inserted a placeholder");
        return placeholderQuiz();
    }

    /** Fills a missing {@code type}: open when there are no options, else MCQ. */
    private static Quiz normalize(Quiz quiz) {
        List<QuizQuestion> fixed = new ArrayList<>();
        for (QuizQuestion q : quiz.questions()) {
            String type = q.type();
            if (type == null || type.isBlank()) {
                boolean hasOptions = q.options() != null && !q.options().isEmpty();
                type = hasOptions ? QuizQuestion.MULTIPLE_CHOICE : QuizQuestion.OPEN;
            }
            fixed.add(new QuizQuestion(q.prompt(), type, q.options(),
                    q.correctIndex(), q.explanation(), q.sampleAnswer()));
        }
        return new Quiz(fixed);
    }

    private static Quiz placeholderQuiz() {
        return new Quiz(List.of(new QuizQuestion(
                "Quiz generation returned no valid JSON — try Refine → Quiz, or a stronger model.",
                QuizQuestion.MULTIPLE_CHOICE, List.of("OK"), 0,
                "The model did not produce parseable quiz JSON.", null)));
    }
}
