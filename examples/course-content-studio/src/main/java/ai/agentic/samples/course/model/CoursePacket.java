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
package ai.agentic.samples.course.model;

/**
 * The generated learning packet for one chapter: an introduction, a quiz and a
 * conclusion, plus an approval flag.
 * <p>
 * It is a mutable JavaBean on purpose: the agent builds it incrementally across
 * its ordered {@code @Action} phases (intro &rarr; quiz &rarr; conclusion),
 * holding it as per-workflow instance state, then publishes it in the
 * {@code @Outcome}. Jakarta JSON Binding uses the no-arg constructor and the
 * getters/setters to (de)serialize it for the refine round-trip and the REST
 * response.
 */
public class CoursePacket {

    private String subject;
    private String chapterTitle;
    private String intro;
    private Quiz quiz;
    private String conclusion;
    private boolean approved;

    public CoursePacket() {
    }

    public CoursePacket(String subject, String chapterTitle) {
        this.subject = subject;
        this.chapterTitle = chapterTitle;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getChapterTitle() {
        return chapterTitle;
    }

    public void setChapterTitle(String chapterTitle) {
        this.chapterTitle = chapterTitle;
    }

    public String getIntro() {
        return intro;
    }

    public void setIntro(String intro) {
        this.intro = intro;
    }

    public Quiz getQuiz() {
        return quiz;
    }

    public void setQuiz(Quiz quiz) {
        this.quiz = quiz;
    }

    public String getConclusion() {
        return conclusion;
    }

    public void setConclusion(String conclusion) {
        this.conclusion = conclusion;
    }

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }
}
