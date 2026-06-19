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

import ee.jakarta.tck.ai.agent.framework.junit.anno.Assertion;
import ee.jakarta.tck.ai.agent.framework.junit.anno.Deployed;
import ee.jakarta.tck.ai.agent.framework.junit.anno.RequiresEngine;
import ee.jakarta.tck.ai.agent.framework.junit.anno.RequiresNoEngine;
import ee.jakarta.tck.ai.agent.framework.stub.LargeLanguageModelStub;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder.Phase;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end smoke test for the Jakarta Agentic AI TCK infrastructure.
 *
 * <p>This test validates that the deployment archive boots, that the CDI
 * container picks up the agent and observes the trigger event, and that the
 * internal infrastructure (stub + trace recorder) is wired correctly.</p>
 *
 * <p><b>Scope limitation</b>: in the absence of a Reference Implementation
 * of the Agentic AI orchestration engine, only the {@code @Trigger} phase
 * is invoked (it is registered as a CDI observer via {@code @Observes}).
 * The {@code @Action} and {@code @Outcome} phases require an engine that
 * scans the agent and dispatches subsequent phases — see
 * {@link #fullLifecycleRequiresReferenceImplementation()} for the assertion
 * to be re-enabled once such an engine is available.</p>
 */
@Deployed
public class AgentSmokeTest {

    @Deployment
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "agent-smoke.war")
                .addClasses(GreetingAgent.class, GreetEvent.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject
    Event<GreetEvent> events;

    @Inject
    LargeLanguageModelStub llm;

    @Inject
    ExecutionTraceRecorder trace;

    @BeforeEach
    public void setUp() {
        llm.reset();
        trace.reset();
    }

    @RequiresNoEngine
    @Assertion(id = "AGENTICAI-SMOKE-001",
               section = "3.2 Agent Lifecycle",
               strategy = "Agent Deployment -> CDI Event Trigger -> Trace records the @Trigger phase")
    public void deploymentTriggersAgentObserver() {
        events.fire(new GreetEvent("Jakarta AI"));

        trace.assertOrder(Phase.TRIGGER);

        ExecutionTraceRecorder.TraceEntry entry = trace.entries().get(0);
        assertEquals("onGreet", entry.methodName(), "Trigger method name");
        assertTrue(entry.args()[0] instanceof GreetEvent, "Trigger should receive the event");
        assertEquals("Jakarta AI", ((GreetEvent) entry.args()[0]).getName(), "Event payload preserved");
    }

    /**
     * Full-lifecycle assertion. Disabled until a Reference Implementation
     * of the Agentic AI orchestration engine is available — plain CDI does
     * not invoke {@code @Action} and {@code @Outcome} methods on its own.
     */
    @RequiresEngine
    @Assertion(id = "AGENTICAI-SMOKE-002",
               section = "3.2 Agent Lifecycle",
               strategy = "Full lifecycle: @Trigger -> @Action -> @Outcome with LLM stub interaction")
    public void fullLifecycleRequiresReferenceImplementation() {
        llm.enqueueResponse("Hello from Stub!");

        events.fire(new GreetEvent("Jakarta AI"));

        trace.assertOrder(Phase.TRIGGER, Phase.ACTION, Phase.OUTCOME);

        assertEquals(1, llm.recordedCalls().size(), "LLM should have been called once");
        LargeLanguageModelStub.RecordedCall lastCall = llm.lastCall();
        assertEquals("Greet {}", lastCall.prompt(), "Unexpected prompt template");
        assertEquals("Jakarta AI", lastCall.params()[0], "Unexpected prompt parameter");
        assertEquals(3, lastCall.overload(), "Should have used query(String, Object...) overload");

        ExecutionTraceRecorder.TraceEntry outcomeEntry = trace.entries().stream()
                .filter(e -> e.phase() == Phase.OUTCOME)
                .findFirst()
                .orElseThrow();
        assertEquals("Hello from Stub!", outcomeEntry.args()[0], "Unexpected response in outcome");
    }
}
