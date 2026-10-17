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
package ee.jakarta.examples.ai.agent.coursecontentstudio.model;

/**
 * The learning packet for one chapter, built up section by section by
 * {@code CourseContentAgent}.
 */
public class CoursePacket {

    private String subject;
    private String chapterTitle;
    private String intro;
    private Quiz quiz;
    private String conclusion;
    private boolean approved;
    private String notice;

    public CoursePacket() {
    }

    public CoursePacket(String subject, String chapterTitle) {
        this.subject = subject;
        this.chapterTitle = chapterTitle;
    }

    public CoursePacket(CoursePacket other) {
        this(other.subject, other.chapterTitle);
        intro = other.intro;
        quiz = other.quiz;
        conclusion = other.conclusion;
        approved = other.approved;
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

    public String getNotice() {
        return notice;
    }

    public void setNotice(String notice) {
        this.notice = notice;
    }

    public boolean hasContent() {
        return intro != null || quiz != null || conclusion != null;
    }
}
