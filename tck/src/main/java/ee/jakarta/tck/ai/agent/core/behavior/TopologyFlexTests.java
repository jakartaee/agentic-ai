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

import ee.jakarta.tck.ai.agent.core.behavior.agents.topologyflex.NoDecisionAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.topologyflex.NoDecisionEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.topologyflex.NoOutcomeAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.topologyflex.NoOutcomeEvent;
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
public class TopologyFlexTests {

    private static final String NO_RI =
            "Requires a Reference Implementation of the Agentic AI engine "
          + "to dispatch @Decision/@Action/@Outcome phases.";

    @Deployment
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "topologyflex.war")
                .addClasses(NoDecisionAgent.class, NoDecisionEvent.class,
                            NoOutcomeAgent.class,  NoOutcomeEvent.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject Event<NoDecisionEvent> noDecisionEvents;
    @Inject Event<NoOutcomeEvent>  noOutcomeEvents;
    @Inject LargeLanguageModelStub llm;
    @Inject ExecutionTraceRecorder trace;

    @BeforeEach
    public void setUp() {
        llm.reset();
        trace.reset();
    }

    @Assertion(id = "AGENTICAI-FLEX-001",
               section = "3.5 Optional Phases",
               strategy = "Agent without @Decision triggers successfully via CDI")
    public void noDecisionAgentTriggerObserved() {
        noDecisionEvents.fire(new NoDecisionEvent("test"));
        assertThat(trace.phases()).containsExactly(Phase.TRIGGER);
    }

    @Assertion(id = "AGENTICAI-FLEX-002",
               section = "3.5 Optional Phases",
               strategy = "Agent without @Outcome triggers successfully via CDI")
    public void noOutcomeAgentTriggerObserved() {
        noOutcomeEvents.fire(new NoOutcomeEvent("test"));
        assertThat(trace.phases()).containsExactly(Phase.TRIGGER);
    }

    @Disabled(NO_RI)
    @Assertion(id = "AGENTICAI-FLEX-003",
               section = "3.5 Optional Phases",
               strategy = "Workflow without @Decision executes T→A→O without error")
    public void workflowWithoutDecisionExecutesActions() {
        noDecisionEvents.fire(new NoDecisionEvent("test"));
        assertThat(trace.phases()).containsExactly(Phase.TRIGGER, Phase.ACTION, Phase.OUTCOME);
    }

    @Disabled(NO_RI)
    @Assertion(id = "AGENTICAI-FLEX-004",
               section = "3.5 Optional Phases",
               strategy = "Workflow without @Outcome completes gracefully after @Action")
    public void workflowWithoutOutcomeCompletesGracefully() {
        noOutcomeEvents.fire(new NoOutcomeEvent("test"));
        assertThat(trace.phases()).containsExactly(Phase.TRIGGER, Phase.DECISION, Phase.ACTION);
    }
}
