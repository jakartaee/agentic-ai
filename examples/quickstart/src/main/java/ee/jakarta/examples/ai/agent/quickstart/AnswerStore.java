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

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Application-scoped holder so the synchronous REST call can read back the
 * answer produced by the agent's {@code @Action} phase.
 */
@ApplicationScoped
public class AnswerStore {

    private final Map<String, String> answers = new ConcurrentHashMap<>();

    public void put(String question, String answer) {
        answers.put(question, answer == null ? "" : answer);
    }

    public String get(String question) {
        return answers.get(question);
    }
}
