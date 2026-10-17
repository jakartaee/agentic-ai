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
import ee.jakarta.examples.ai.agent.coursecontentstudio.model.LessonApproved;
import ee.jakarta.examples.ai.agent.coursecontentstudio.model.PublishedLesson;
import ee.jakarta.examples.ai.agent.coursecontentstudio.model.Quiz;
import ee.jakarta.examples.ai.agent.coursecontentstudio.model.QuizQuestion;
import jakarta.ai.agent.LLMException;
import jakarta.ai.agent.LargeLanguageModel;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.ObserverException;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.ServiceUnavailableException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * REST surface for the studio: generate and refine fire the
 * {@link CoursePacketRequest} event and return the resulting packet, approve
 * fires {@link LessonApproved}, and the lesson and grading endpoints serve the
 * student view.
 */
@Path("/")
@RequestScoped
public class CourseResource {

    private static final Logger LOGGER = Logger.getLogger(CourseResource.class.getName());

    @Inject
    Event<CoursePacketRequest> trigger;

    @Inject
    Event<LessonApproved> approvals;

    @Inject
    PacketStore store;

    @Inject
    PublishedLessonStore publishedLessons;

    @Inject
    LargeLanguageModel model;

    @Inject
    SubjectRubric rubric;

    @Inject
    ProgressTracker progress;

    @GET
    @Path("subjects")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> subjects() {
        return rubric.subjects();
    }

    @GET
    @Path("progress/{runId}")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void progress(@PathParam("runId") String runId,
                         @Context SseEventSink sink,
                         @Context Sse sse) {
        progress.register(runId, sink, sse);
    }

    @GET
    @Path("packet")
    @Produces(MediaType.APPLICATION_JSON)
    public Response current() {
        return respond(store.current());
    }

    @POST
    @Path("packet/generate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response generate(@NotNull GenerateRequest body) {
        fire(body.runId(), () -> trigger.fire(CoursePacketRequest.generate(
                body.subject(), body.chapterTitle(), body.chapterBody(), body.runId())));
        return respond(store.current());
    }

    @POST
    @Path("packet/refine-section")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response refineSection(@NotNull RefineSectionRequest body) {
        CoursePacket base = store.current();
        if (base == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new Message("Nothing to refine yet — generate a packet first."))
                    .build();
        }
        if (body.instruction() == null || body.instruction().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new Message("A refine instruction is required.")).build();
        }
        String section = body.section() == null || body.section().isBlank()
                ? "all" : body.section();
        fire(body.runId(), () -> trigger.fire(CoursePacketRequest.refine(
                base, section, body.instruction(), body.runId())));
        return respond(store.current());
    }

    @POST
    @Path("packet/approve")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response approve(@NotNull ApproveRequest body) {
        CoursePacket approved = store.approve();
        if (approved == null) {
            return respond(null);
        }
        fire(body.runId(), () -> approvals.fire(new LessonApproved(approved, body.runId())));
        return respond(approved);
    }

    @GET
    @Path("lesson")
    @Produces(MediaType.APPLICATION_JSON)
    public Response lesson() {
        PublishedLesson lesson = publishedLessons.current();
        if (lesson == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new Message("No lesson has been published yet.")).build();
        }
        return Response.ok(lesson).build();
    }

    @POST
    @Path("quiz/grade")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response grade(GradeRequest body) {
        PublishedLesson lesson = publishedLessons.current();
        Quiz quiz = lesson == null ? null : lesson.quiz();
        if (quiz == null || quiz.questions() == null
                || body == null || body.questionIndex() < 0
                || body.questionIndex() >= quiz.questions().size()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new Message("Unknown question.")).build();
        }
        QuizQuestion question = quiz.questions().get(body.questionIndex());
        if (!question.isOpen()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new Message("That question is not an open question.")).build();
        }
        int similarity;
        try {
            similarity = gradeSimilarity(question.prompt(), question.sampleAnswer(),
                    body.studentAnswer());
        } catch (LLMException e) {
            throw llmUnavailable(e);
        }
        String verdict = similarity >= 70 ? "correct"
                : similarity >= 50 ? "partial" : "incorrect";
        return Response.ok(new GradeResponse(similarity, verdict, question.sampleAnswer())).build();
    }

    private int gradeSimilarity(String prompt, String modelAnswer, String studentAnswer) {
        if (studentAnswer == null || studentAnswer.isBlank()
                || modelAnswer == null || modelAnswer.isBlank()) {
            return 0;
        }
        Similarity similarity = model.query(
                "Grade a student's open answer. Question: {}\nModel answer: {}\n"
                        + "Student answer: {}\nRate from 0 to 100 how well the student's answer "
                        + "matches the model answer in meaning. Return ONLY JSON: {\"similarity\": N}",
                Similarity.class,
                prompt, modelAnswer, studentAnswer);
        return Math.max(0, Math.min(100, similarity.similarity()));
    }

    private void fire(String runId, Runnable workflow) {
        try {
            workflow.run();
        } catch (ObserverException wrapper) {
            Throwable cause = wrapper.getCause();
            if (cause instanceof ConstraintViolationException violations) {
                throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST)
                        .entity(new Message(describe(violations))).build());
            }
            if (cause instanceof LLMException) {
                throw llmUnavailable(cause);
            }
            throw wrapper;
        } finally {
            progress.complete(runId);
        }
    }

    private static ServiceUnavailableException llmUnavailable(Throwable cause) {
        LOGGER.log(Level.WARNING, "LLM call failed", cause);
        return new ServiceUnavailableException(
                Response.status(Response.Status.SERVICE_UNAVAILABLE)
                        .entity(new Message("The LLM backend failed: " + cause.getMessage()))
                        .build());
    }

    private static String describe(ConstraintViolationException e) {
        return e.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
    }

    private Response respond(CoursePacket packet) {
        if (packet == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new Message("No packet available.")).build();
        }
        return Response.ok(packet).build();
    }

    public record GenerateRequest(String subject, String chapterTitle, String chapterBody,
                                  String runId) {
    }

    public record RefineSectionRequest(String section, String instruction, String runId) {
    }

    public record ApproveRequest(String runId) {
    }

    public record GradeRequest(int questionIndex, String studentAnswer) {
    }

    public record GradeResponse(int similarity, String verdict, String sampleAnswer) {
    }

    public record Similarity(int similarity) {
    }

    public record Message(String message) {
    }
}
