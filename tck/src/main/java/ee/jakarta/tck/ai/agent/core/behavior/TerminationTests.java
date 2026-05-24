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

import ee.jakarta.tck.ai.agent.core.behavior.agents.termination.BooleanTerminationAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.termination.BooleanTerminationEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.termination.ObjectTerminationAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.termination.ObjectTerminationDecision;
import ee.jakarta.tck.ai.agent.core.behavior.agents.termination.ObjectTerminationEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.termination.ResultTerminationAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.termination.ResultTerminationEvent;
import ee.jakarta.tck.ai.agent.framework.junit.anno.Assertion;
import ee.jakarta.tck.ai.agent.framework.junit.anno.Deployed;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder.Phase;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.TestInstance;

import static org.assertj.core.api.Assertions.assertThat;

@Deployed
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TerminationTests {

    private static final String NO_RI =
            "Requires a Reference Implementation of the Agentic AI engine "
          + "to dispatch @Decision/@Action/@Outcome phases.";

    @Deployment
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "termination.war")
                .addClasses(BooleanTerminationAgent.class, BooleanTerminationEvent.class,
                            ResultTerminationAgent.class,  ResultTerminationEvent.class,
                            ObjectTerminationAgent.class,  ObjectTerminationEvent.class,
                            ObjectTerminationDecision.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject Event<BooleanTerminationEvent> booleanEvents;
    @Inject Event<ResultTerminationEvent>  resultEvents;
    @Inject Event<ObjectTerminationEvent>  objectEvents;
    @Inject ExecutionTraceRecorder trace;

    @Assertion(id = "AGENTICAI-TERM-001",
               section = "3.3 Decision Phase",
               strategy = "Trigger is observed in the boolean-termination agent")
    public void booleanAgentTriggerObserved() {
        trace.reset();
        booleanEvents.fire(new BooleanTerminationEvent("x"));
        assertThat(trace.phases()).containsExactly(Phase.TRIGGER);
    }

    @Assertion(id = "AGENTICAI-TERM-002",
               section = "3.3 Decision Phase",
               strategy = "Trigger is observed in the Result-termination agent")
    public void resultAgentTriggerObserved() {
        trace.reset();
        resultEvents.fire(new ResultTerminationEvent("x"));
        assertThat(trace.phases()).containsExactly(Phase.TRIGGER);
    }

    @Assertion(id = "AGENTICAI-TERM-003",
               section = "3.3 Decision Phase",
               strategy = "Trigger is observed in the Object-termination agent")
    public void objectAgentTriggerObserved() {
        trace.reset();
        objectEvents.fire(new ObjectTerminationEvent("x"));
        assertThat(trace.phases()).containsExactly(Phase.TRIGGER);
    }

    @Disabled(NO_RI)
    @Assertion(id = "AGENTICAI-TERM-004",
               section = "3.3 Decision Phase",
               strategy = "Boolean false from @Decision halts all downstream phases")
    public void booleanFalseTerminatesWorkflow() {
        trace.reset();
        booleanEvents.fire(new BooleanTerminationEvent("x"));
        assertThat(trace.phases()).containsExactly(Phase.TRIGGER, Phase.DECISION);
    }

    @Disabled(NO_RI)
    @Assertion(id = "AGENTICAI-TERM-005",
               section = "3.3 Decision Phase",
               strategy = "Result(success=false) from @Decision halts all downstream phases")
    public void resultFalseTerminatesWorkflow() {
        trace.reset();
        resultEvents.fire(new ResultTerminationEvent("x"));
        assertThat(trace.phases()).containsExactly(Phase.TRIGGER, Phase.DECISION);
    }

    @Disabled(NO_RI)
    @Assertion(id = "AGENTICAI-TERM-006",
               section = "3.3 Decision Phase",
               strategy = "Object null from @Decision halts all downstream phases")
    public void objectNullTerminatesWorkflow() {
        trace.reset();
        objectEvents.fire(new ObjectTerminationEvent("x"));
        assertThat(trace.phases()).containsExactly(Phase.TRIGGER, Phase.DECISION);
    }
}
