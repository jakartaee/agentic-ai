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
package ee.jakarta.examples.ai.agent.quickstart;

import jakarta.ai.agent.Action;
import jakarta.ai.agent.Agent;
import jakarta.ai.agent.Decision;
import jakarta.ai.agent.LargeLanguageModel;
import jakarta.ai.agent.Outcome;
import jakarta.ai.agent.Trigger;
import jakarta.inject.Inject;
import jakarta.validation.Valid;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A compact Jakarta Agentic AI agent: answers a question with the configured LLM
 * backend. Exercises all four specification phases &mdash; {@code @Trigger},
 * {@code @Decision}, {@code @Action}, {@code @Outcome} &mdash; and logs each so
 * the workflow is visible in {@code server.log}.
 * <p>
 * Default scope is {@code @WorkflowScoped} (applied by the runtime extension).
 */
@Agent(name = "QuestionAgent", description = "Answers a question using the configured LLM backend.")
public class QuestionAgent {

    private static final Logger LOGGER = Logger.getLogger(QuestionAgent.class.getName());

    @Inject
    LargeLanguageModel model;

    @Inject
    AnswerStore answers;

    @Trigger
    void onQuestion(@Valid Question question) {
        LOGGER.log(Level.INFO, "[TRIGGER] question received: {0}", question.text());
    }

    @Decision
    boolean notYetAnswered(Question question) {
        boolean proceed = answers.get(question.text()) == null;
        LOGGER.log(Level.INFO, "[DECISION] not yet answered: {0}", proceed);
        return proceed;
    }

    @Action
    Answer generate(Question question) {
        LOGGER.log(Level.INFO, "[ACTION] querying LLM for: {0}", question.text());
        return new Answer(model.query("Answer concisely in one short paragraph: {}",
                question.text()));
    }

    @Outcome
    void store(Question question, Answer answer) {
        answers.put(question.text(), answer.text());
        LOGGER.log(Level.INFO, "[OUTCOME] stored answer: {0}", answer.text());
    }
}
