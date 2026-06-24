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

import ee.jakarta.tck.ai.agent.core.behavior.agents.datapropagation.ActionOutput;
import ee.jakarta.tck.ai.agent.core.behavior.agents.datapropagation.DataPropagationAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.datapropagation.DataPropagationEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.datapropagation.DecisionOutput;
import ee.jakarta.tck.ai.agent.core.behavior.agents.datapropagation.TriggerOutput;
import ee.jakarta.tck.ai.agent.framework.junit.anno.Assertion;
import ee.jakarta.tck.ai.agent.framework.junit.anno.Deployed;
import ee.jakarta.tck.ai.agent.framework.junit.anno.RequiresImplementation;
import ee.jakarta.tck.ai.agent.framework.junit.anno.RequiresNoImplementation;
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

import static org.assertj.core.api.Assertions.assertThat;

@Deployed
public class DataPropagationTests {

    @Deployment
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "datapropagation.war")
                .addClasses(DataPropagationAgent.class, DataPropagationEvent.class,
                            TriggerOutput.class, DecisionOutput.class, ActionOutput.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject Event<DataPropagationEvent> events;
    @Inject LargeLanguageModelStub llm;
    @Inject ExecutionTraceRecorder trace;

    @BeforeEach
    public void setUp() {
        llm.reset();
        trace.reset();
    }

    @RequiresNoImplementation
    @Assertion(id = "AGENTICAI-DATA-001",
               section = "3.4 Data Propagation",
               strategy = "Trigger fires and records its phase before the implementation dispatches further phases")
    public void triggerIsObserved() {
        events.fire(new DataPropagationEvent("input"));
        assertThat(trace.phases()).containsExactly(Phase.TRIGGER);
    }

    @RequiresImplementation
    @Assertion(id = "AGENTICAI-DATA-002",
               section = "3.4 Data Propagation",
               strategy = "TriggerOutput returned by @Trigger is injectable as a parameter in @Decision")
    public void triggerOutputIsInjectableInDecision() {
        llm.enqueueResponse("ok");
        events.fire(new DataPropagationEvent("input"));
        assertThat(trace.entries().get(1).args()[1]).isInstanceOf(TriggerOutput.class);
    }

    @RequiresImplementation
    @Assertion(id = "AGENTICAI-DATA-003",
               section = "3.4 Data Propagation",
               strategy = "DecisionOutput returned by @Decision is injectable as a parameter in @Action")
    public void decisionOutputIsInjectableInAction() {
        llm.enqueueResponse("ok");
        events.fire(new DataPropagationEvent("input"));
        assertThat(trace.entries().get(2).args()[1]).isInstanceOf(DecisionOutput.class);
    }

    @RequiresImplementation
    @Assertion(id = "AGENTICAI-DATA-004",
               section = "3.4 Data Propagation",
               strategy = "@Outcome can inject objects from all preceding phases simultaneously")
    public void allPhaseOutputsAreInjectableInOutcome() {
        llm.enqueueResponse("ok");
        events.fire(new DataPropagationEvent("input"));

        assertThat(trace.phases())
                .containsExactly(Phase.TRIGGER, Phase.DECISION, Phase.ACTION, Phase.OUTCOME);

        Object[] outcomeArgs = trace.entries().get(3).args();
        assertThat(outcomeArgs[0]).isInstanceOf(DataPropagationEvent.class);
        assertThat(outcomeArgs[1]).isInstanceOf(TriggerOutput.class);
        assertThat(outcomeArgs[2]).isInstanceOf(DecisionOutput.class);
        assertThat(outcomeArgs[3]).isInstanceOf(ActionOutput.class);
    }
}
