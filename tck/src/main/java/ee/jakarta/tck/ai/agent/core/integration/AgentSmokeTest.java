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
import ee.jakarta.tck.ai.agent.framework.junit.anno.RequiresImplementation;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * End-to-end smoke test: the archive deploys, the trigger event reaches the
 * agent, the implementation dispatches the remaining phases and the stub and
 * trace recorder are wired into the deployment.
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

    @RequiresImplementation
    @Assertion(id = "AGENTICAI-SMOKE-002",
               section = "agent-lifecycle",
               strategy = "Full lifecycle: @Trigger -> @Action -> @Outcome with LLM stub interaction")
    public void fullLifecycleRequiresCompatibleImplementation() {
        llm.enqueueResponse("Hello from Stub!");

        events.fire(new GreetEvent("Jakarta AI"));

        assertEquals(List.of(Phase.TRIGGER, Phase.ACTION, Phase.OUTCOME), trace.phases());

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
