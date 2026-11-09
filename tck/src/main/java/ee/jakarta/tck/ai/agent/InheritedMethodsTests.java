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
package ee.jakarta.tck.ai.agent;

import ee.jakarta.tck.ai.agent.agents.inheritedmethods.BasePhaseAgent;
import ee.jakarta.tck.ai.agent.agents.inheritedmethods.InheritEvent;
import ee.jakarta.tck.ai.agent.agents.inheritedmethods.InheritingAgent;
import ee.jakarta.tck.ai.agent.agents.inheritedmethods.OverrideEvent;
import ee.jakarta.tck.ai.agent.agents.inheritedmethods.OverridingAgent;
import ee.jakarta.tck.ai.agent.framework.ExecutionTraceRecorder;
import ee.jakarta.tck.ai.agent.framework.ExecutionTraceRecorder.Phase;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * TCK coverage for phase-method inheritance. A phase method declared on a
 * superclass is inherited and participates in the workflow, while an unannotated
 * override does not participate.
 *
 * <p>Uses {@code @TestInstance(PER_CLASS)} with inline {@code trace.reset()} because
 * {@code @BeforeEach} does not run reliably between methods of an Arquillian test
 * class inside the container.</p>
 */
@ExtendWith(ArquillianExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class InheritedMethodsTests {

    @Deployment
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "inheritedmethods.war")
                .addClasses(
                        BasePhaseAgent.class, InheritingAgent.class, InheritEvent.class,
                        OverridingAgent.class, OverrideEvent.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject Event<InheritEvent> inheritEvents;
    @Inject Event<OverrideEvent> overrideEvents;
    @Inject ExecutionTraceRecorder trace;
    /**
     * A phase method declared on a superclass is inherited and participates in the workflow.
     *
     * <p>Specification section {@code inherited-methods}.
     */
    @Test
    public void inheritedActionParticipates() {
        trace.reset();
        inheritEvents.fire(new InheritEvent("input"));
        assertEquals(List.of(Phase.TRIGGER, Phase.ACTION), trace.phases());
    }
    /**
     * An override that drops the phase annotation does not participate in the workflow.
     *
     * <p>Specification section {@code inherited-methods}.
     */
    @Test
    public void unannotatedOverrideDoesNotParticipate() {
        trace.reset();
        overrideEvents.fire(new OverrideEvent("input"));
        assertEquals(List.of(Phase.TRIGGER), trace.phases());
        assertFalse(trace.phases().contains(Phase.ACTION));
    }
}