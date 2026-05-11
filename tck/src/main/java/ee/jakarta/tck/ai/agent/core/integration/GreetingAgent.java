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
package ee.jakarta.tck.ai.agent.core.integration;

import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder.Phase;
import jakarta.ai.agent.Action;
import jakarta.ai.agent.Agent;
import jakarta.ai.agent.LargeLanguageModel;
import jakarta.ai.agent.Outcome;
import jakarta.ai.agent.Trigger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * Minimal agent for the smoke test.
 *
 * <p>Annotated with {@code @ApplicationScoped} so that the CDI container
 * discovers the class as a bean and registers the {@code @Observes} method
 * as an event observer. {@code @Agent} alone is not a CDI bean defining
 * annotation.</p>
 */
@Agent
@ApplicationScoped
public class GreetingAgent {

    @Inject
    LargeLanguageModel llm;

    @Inject
    ExecutionTraceRecorder trace;

    @Trigger
    public void onGreet(@Observes GreetEvent event) {
        trace.record(Phase.TRIGGER, "onGreet", event);
    }

    @Action
    public String generateGreeting(GreetEvent event) {
        trace.record(Phase.ACTION, "generateGreeting", event);
        return llm.query("Greet {}", event.getName());
    }

    @Outcome
    public void finished(String response) {
        trace.record(Phase.OUTCOME, "finished", response);
    }
}
