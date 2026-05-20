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
package ee.jakarta.tck.ai.agent.core.behavior;

import ee.jakarta.tck.ai.agent.core.behavior.agents.contextinjection.ContextInjectionAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.contextinjection.ContextInjectionEvent;
import ee.jakarta.tck.ai.agent.framework.junit.anno.Assertion;
import ee.jakarta.tck.ai.agent.framework.junit.anno.Deployed;
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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.TestInstance;

import static org.assertj.core.api.Assertions.assertThat;

@Deployed
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ContextInjectionTests {

    private static final String NO_RI =
            "Requires a Reference Implementation of the Agentic AI engine "
          + "to dispatch @Decision/@Action/@Outcome phases.";

    @Deployment
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "contextinjection.war")
                .addClasses(ContextInjectionAgent.class, ContextInjectionEvent.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject Event<ContextInjectionEvent> events;
    @Inject LargeLanguageModelStub llm;
    @Inject ExecutionTraceRecorder trace;

    @BeforeEach
    public void setUp() {
        llm.reset();
        trace.reset();
    }

    @Assertion(id = "AGENTICAI-CTX-001",
               section = "3.2 Agent Lifecycle",
               strategy = "LargeLanguageModel is injectable as a direct method parameter of @Trigger")
    public void llmIsInjectedAsTriggerParameter() {
        llm.reset();
        trace.reset();
        llm.enqueueResponse("classified");
        events.fire(new ContextInjectionEvent("payload"));

        assertThat(trace.phases()).containsExactly(Phase.TRIGGER);
        assertThat(llm.recordedCalls()).hasSize(1);
        assertThat(llm.lastCall().prompt()).isEqualTo("classify {}");
        assertThat(llm.lastCall().params()).containsExactly("payload");
    }

    @Assertion(id = "AGENTICAI-CTX-002",
               section = "3.2 Agent Lifecycle",
               strategy = "Triggering event payload is accessible in @Trigger and preserved in the trace")
    public void eventPayloadIsPreservedInTrigger() {
        llm.reset();
        trace.reset();
        llm.enqueueResponse("ok");
        events.fire(new ContextInjectionEvent("my-payload"));

        ContextInjectionEvent recorded = (ContextInjectionEvent) trace.entries().get(0).args()[0];
        assertThat(recorded.payload()).isEqualTo("my-payload");
    }

    @Disabled(NO_RI)
    @Assertion(id = "AGENTICAI-CTX-003",
               section = "3.4 Data Propagation",
               strategy = "Triggering event is available for injection in @Action")
    public void eventIsInjectableInAction() {
        events.fire(new ContextInjectionEvent("payload"));
        assertThat(trace.phases()).contains(Phase.ACTION);
        assertThat(trace.entries().stream()
                .filter(e -> e.phase() == Phase.ACTION)
                .findFirst()
                .orElseThrow()
                .args()[0]).isInstanceOf(ContextInjectionEvent.class);
    }

    @Disabled(NO_RI)
    @Assertion(id = "AGENTICAI-CTX-004",
               section = "3.4 Data Propagation",
               strategy = "LargeLanguageModel is injectable as a direct method parameter of @Action")
    public void llmIsInjectedInAction() {
        llm.enqueueResponse("classified");
        events.fire(new ContextInjectionEvent("payload"));
        assertThat(trace.phases()).contains(Phase.ACTION);
    }
}
