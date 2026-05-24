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

import ee.jakarta.tck.ai.agent.core.behavior.agents.phaseordering.PhaseOrderingAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.phaseordering.PhaseOrderingEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.phaseordering.TriggerOutput;
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
public class PhaseOrderingTests {

    private static final String NO_RI =
            "Requires a Reference Implementation of the Agentic AI engine "
          + "to dispatch @Decision/@Action/@Outcome phases.";

    @Deployment
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "phaseordering.war")
                .addClasses(PhaseOrderingAgent.class, PhaseOrderingEvent.class, TriggerOutput.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject Event<PhaseOrderingEvent> events;
    @Inject LargeLanguageModelStub llm;
    @Inject ExecutionTraceRecorder trace;

    @BeforeEach
    public void setUp() {
        llm.reset();
        trace.reset();
    }

    @Assertion(id = "AGENTICAI-ORDER-001",
               section = "3.2 Agent Lifecycle",
               strategy = "Trigger event is observed; trace records TRIGGER with method name and payload")
    public void triggerIsObservedAndRecorded() {
        events.fire(new PhaseOrderingEvent("test-payload"));

        assertThat(trace.phases()).containsExactly(Phase.TRIGGER);
        assertThat(trace.entries().get(0).methodName()).isEqualTo("onEvent");
        assertThat(trace.entries().get(0).args()[0])
                .isInstanceOf(PhaseOrderingEvent.class)
                .extracting(e -> ((PhaseOrderingEvent) e).payload())
                .isEqualTo("test-payload");
    }

    @Disabled(NO_RI)
    @Assertion(id = "AGENTICAI-ORDER-002",
               section = "3.2 Agent Lifecycle",
               strategy = "Full T→D→A→O pipeline executes in strict declaration order")
    public void fullPipelineExecutesInStrictOrder() {
        llm.enqueueResponse("proceed");
        events.fire(new PhaseOrderingEvent("test-payload"));

        assertThat(trace.phases())
                .containsExactly(Phase.TRIGGER, Phase.DECISION, Phase.ACTION, Phase.OUTCOME);
    }

    @Disabled(NO_RI)
    @Assertion(id = "AGENTICAI-ORDER-003",
               section = "3.2 Agent Lifecycle",
               strategy = "Trigger return value (non-void) is available for injection in Decision")
    public void triggerReturnValueIsInjectableInDecision() {
        llm.enqueueResponse("proceed");
        events.fire(new PhaseOrderingEvent("test-payload"));

        Object[] decisionArgs = trace.entries().get(1).args();
        assertThat(decisionArgs[0]).isInstanceOf(PhaseOrderingEvent.class);
        assertThat(decisionArgs[1])
                .isInstanceOf(TriggerOutput.class)
                .extracting(o -> ((TriggerOutput) o).fromTrigger())
                .isEqualTo("trigger:test-payload");
    }
}
