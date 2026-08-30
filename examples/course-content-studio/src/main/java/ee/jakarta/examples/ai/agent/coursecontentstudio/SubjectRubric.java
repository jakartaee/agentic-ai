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

import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Locale;

/**
 * Supplies a subject-specific writing rubric that is prepended to the LLM
 * prompts. This is how a single agent specialises its output per subject
 * (maths shows worked steps, physics insists on units, English focuses on
 * usage) without branching into multiple agents.
 */
@ApplicationScoped
public class SubjectRubric {

    /** Subjects offered in the UI dropdown. */
    public List<String> subjects() {
        return List.of("Mathematics", "Physics", "English");
    }

    /** The guidance prefix for a subject; falls back to a generic rubric. */
    public String forSubject(String subject) {
        String key = subject == null ? "" : subject.trim().toLowerCase(Locale.ROOT);
        return switch (key) {
            case "mathematics", "math", "maths" -> """
                    You are a mathematics teacher. Be precise with notation, show
                    worked steps, and prefer concrete numeric examples. Assume the
                    reader has seen the definitions but needs intuition. Write every
                    formula in LaTeX: inline as $...$ and display equations as $$...$$.""";
            case "physics" -> """
                    You are a physics teacher. Always state units, connect formulas
                    to physical intuition, and give a real-world example the reader
                    can picture. Write every formula in LaTeX: inline as $...$ and
                    display equations as $$...$$.""";
            case "english" -> """
                    You are an English language teacher. Focus on grammar, usage and
                    style. Use short illustrative sentences and point out common
                    mistakes.""";
            default -> """
                    You are an experienced teacher. Write clearly for a motivated
                    student, using concrete examples.""";
        };
    }
}
