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

import ee.jakarta.tck.ai.agent.core.behavior.agents.inheritance.BasePhaseAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.inheritance.InheritEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.inheritance.InheritingAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.inheritance.OverrideEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.inheritance.OverridingAgent;
import ee.jakarta.tck.ai.agent.framework.junit.anno.Assertion;
import ee.jakarta.tck.ai.agent.framework.junit.anno.Deployed;
import ee.jakarta.tck.ai.agent.framework.junit.anno.RequiresImplementation;
import ee.jakarta.tck.ai.agent.framework.junit.anno.RequiresNoImplementation;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder.Phase;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.TestInstance;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TCK coverage for phase-method inheritance. A phase method declared on a
 * superclass is inherited and participates in the workflow, while an unannotated
 * override does not participate.
 *
 * <p>Uses {@code @TestInstance(PER_CLASS)} with inline {@code trace.reset()} because
 * {@code @BeforeEach} does not run reliably between methods of a {@code @Deployed}
 * class inside the container.</p>
 */
@Deployed
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class InheritedPhaseTests {

    @Deployment
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "inheritedphase.war")
                .addClasses(
                        BasePhaseAgent.class, InheritingAgent.class, InheritEvent.class,
                        OverridingAgent.class, OverrideEvent.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject Event<InheritEvent> inheritEvents;
    @Inject Event<OverrideEvent> overrideEvents;
    @Inject ExecutionTraceRecorder trace;

    @RequiresNoImplementation
    @Assertion(id = "AGENTICAI-INHERIT-001-PRECONDITION",
               section = "agent-lifecycle",
               strategy = "Trigger fires and records its phase before the implementation dispatches further phases")
    public void inheritingAgentTriggerIsObserved() {
        trace.reset();
        inheritEvents.fire(new InheritEvent("input"));
        assertThat(trace.phases()).containsExactly(Phase.TRIGGER);
    }

    @RequiresImplementation
    @Assertion(id = "AGENTICAI-INHERIT-001",
               section = "agent-lifecycle",
               strategy = "A phase method declared on a superclass is inherited and participates in the workflow")
    public void inheritedActionParticipates() {
        trace.reset();
        inheritEvents.fire(new InheritEvent("input"));
        assertThat(trace.phases()).containsExactly(Phase.TRIGGER, Phase.ACTION);
    }

    @RequiresNoImplementation
    @Assertion(id = "AGENTICAI-INHERIT-002-PRECONDITION",
               section = "agent-lifecycle",
               strategy = "Trigger fires and records its phase before the implementation dispatches further phases")
    public void overridingAgentTriggerIsObserved() {
        trace.reset();
        overrideEvents.fire(new OverrideEvent("input"));
        assertThat(trace.phases()).containsExactly(Phase.TRIGGER);
    }

    @RequiresImplementation
    @Assertion(id = "AGENTICAI-INHERIT-002",
               section = "agent-lifecycle",
               strategy = "An override that drops the phase annotation does not participate in the workflow")
    public void unannotatedOverrideDoesNotParticipate() {
        trace.reset();
        overrideEvents.fire(new OverrideEvent("input"));
        assertThat(trace.phases()).containsExactly(Phase.TRIGGER);
        assertThat(trace.phases()).doesNotContain(Phase.ACTION);
    }
}