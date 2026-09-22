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

import ee.jakarta.tck.ai.agent.core.behavior.agents.eventobservation.AppScopedObserverAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.eventobservation.EventObservationTriggerEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.eventobservation.GeneralObservationEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.eventobservation.WorkflowScopedObserverAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.eventobservation.WorkflowScopedTriggerEvent;
import ee.jakarta.tck.ai.agent.framework.junit.anno.Assertion;
import ee.jakarta.tck.ai.agent.framework.junit.anno.Deployed;
import ee.jakarta.tck.ai.agent.framework.junit.anno.RequiresImplementation;
import ee.jakarta.tck.ai.agent.framework.junit.anno.RequiresNoImplementation;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder.Phase;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder.TraceEntry;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.TestInstance;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TCK coverage for scope-dependent event observation. An {@code @ApplicationScoped}
 * agent may observe events through both a {@code @Trigger} method and a general
 * {@code @Observes} observer, whereas a {@code @WorkflowScoped} agent observes only
 * through its {@code @Trigger} method.
 *
 * <p>Uses {@code @TestInstance(PER_CLASS)} with inline {@code trace.reset()} because
 * {@code @BeforeEach} does not run reliably between methods of a {@code @Deployed}
 * class inside the container.</p>
 */
@Deployed
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EventObservationTests {

    @Deployment
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "eventobservation.war")
                .addClasses(
                        AppScopedObserverAgent.class, EventObservationTriggerEvent.class,
                        GeneralObservationEvent.class, WorkflowScopedObserverAgent.class,
                        WorkflowScopedTriggerEvent.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject Event<EventObservationTriggerEvent> triggerEvents;
    @Inject Event<GeneralObservationEvent> generalEvents;
    @Inject Event<WorkflowScopedTriggerEvent> workflowScopedEvents;
    @Inject ExecutionTraceRecorder trace;

    private List<String> recordedMethodNames() {
        return trace.entries().stream().map(TraceEntry::methodName).toList();
    }

    @RequiresNoImplementation
    @Assertion(id = "AGENTICAI-EVENT-OBS-001",
               section = "cdi-events",
               strategy = "An @ApplicationScoped agent observes an event through a general @Observes method")
    public void applicationScopedAgentObservesViaGeneralObserver() {
        trace.reset();
        generalEvents.fire(new GeneralObservationEvent("input"));
        assertThat(recordedMethodNames()).containsExactly("onGeneralEvent");
    }

    @RequiresNoImplementation
    @Assertion(id = "AGENTICAI-EVENT-OBS-002",
               section = "cdi-events",
               strategy = "An @ApplicationScoped agent observes an event through its @Trigger method")
    public void applicationScopedAgentObservesViaTrigger() {
        trace.reset();
        triggerEvents.fire(new EventObservationTriggerEvent("input"));
        assertThat(trace.phases()).containsExactly(Phase.TRIGGER);
        assertThat(recordedMethodNames()).containsExactly("onTriggerEvent");
    }

    @RequiresImplementation
    @Assertion(id = "AGENTICAI-EVENT-OBS-003",
               section = "cdi-events",
               strategy = "A @WorkflowScoped agent observes an event through its @Trigger method")
    public void workflowScopedAgentObservesViaTrigger() {
        trace.reset();
        workflowScopedEvents.fire(new WorkflowScopedTriggerEvent("input"));
        assertThat(trace.phases()).containsExactly(Phase.TRIGGER);
        assertThat(recordedMethodNames()).containsExactly("onTriggerEvent");
    }
}