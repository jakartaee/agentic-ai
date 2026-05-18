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

import ee.jakarta.tck.ai.agent.core.behavior.agents.voidphases.VoidPhasesAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.voidphases.VoidPhasesEvent;
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

import static org.assertj.core.api.Assertions.assertThat;

@Deployed
public class VoidPhasesTests {

    private static final String NO_RI =
            "Requires a Reference Implementation of the Agentic AI engine "
          + "to dispatch @Decision/@Action/@Outcome phases.";

    @Deployment
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "voidphases.war")
                .addClasses(VoidPhasesAgent.class, VoidPhasesEvent.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject Event<VoidPhasesEvent> events;
    @Inject LargeLanguageModelStub llm;
    @Inject ExecutionTraceRecorder trace;

    @BeforeEach
    public void setUp() {
        llm.reset();
        trace.reset();
    }

    @Assertion(id = "AGENTICAI-VOID-001",
               section = "3.2 Agent Lifecycle",
               strategy = "void @Trigger is observed and recorded without error")
    public void voidTriggerIsObserved() {
        events.fire(new VoidPhasesEvent("test"));
        assertThat(trace.phases()).containsExactly(Phase.TRIGGER);
    }

    @Disabled(NO_RI)
    @Assertion(id = "AGENTICAI-VOID-002",
               section = "3.2 Agent Lifecycle",
               strategy = "void @Trigger does not pollute the injection context; @Decision receives only the original event")
    public void voidTriggerDoesNotPollutInjectionContext() {
        events.fire(new VoidPhasesEvent("test"));

        assertThat(trace.phases())
                .containsExactly(Phase.TRIGGER, Phase.DECISION, Phase.ACTION, Phase.OUTCOME);
        assertThat(trace.entries().get(1).args()).hasSize(1)
                .allSatisfy(arg -> assertThat(arg).isInstanceOf(VoidPhasesEvent.class));
    }

    @Disabled(NO_RI)
    @Assertion(id = "AGENTICAI-VOID-003",
               section = "3.3 Action Phase",
               strategy = "void @Action does not break the injection context for @Outcome")
    public void voidActionDoesNotBreakOutcome() {
        events.fire(new VoidPhasesEvent("test"));

        assertThat(trace.phases()).contains(Phase.OUTCOME);
        assertThat(trace.entries().stream()
                .filter(e -> e.phase() == Phase.OUTCOME)
                .findFirst()
                .orElseThrow()
                .args()[0]).isInstanceOf(VoidPhasesEvent.class);
    }
}
