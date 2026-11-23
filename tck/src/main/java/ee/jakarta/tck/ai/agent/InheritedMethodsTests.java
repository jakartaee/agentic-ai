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

import ee.jakarta.tck.ai.agent.framework.ExecutionTraceRecorder;
import ee.jakarta.tck.ai.agent.framework.ExecutionTraceRecorder.Phase;
import jakarta.ai.agent.Action;
import jakarta.ai.agent.Agent;
import jakarta.ai.agent.Trigger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
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
                        BasePhaseAgent.class,
                        InheritingAgent.class,
                        InheritEvent.class,
                        OverridingAgent.class,
                        OverrideEvent.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject
    Event<InheritEvent> inheritEvents;

    @Inject
    Event<OverrideEvent> overrideEvents;

    @Inject
    ExecutionTraceRecorder trace;

    /**
     * Superclass, not itself an {@code @Agent}, that declares an {@code @Action}. Whether the
     * phase runs depends on whether the agent subclass overrides it.
     */
    public static class BasePhaseAgent {

        @Inject
        protected ExecutionTraceRecorder trace;

        @Action
        public void act() {
            trace.record(Phase.ACTION, "act");
        }
    }

    public record InheritEvent(String payload) {}

    /** Inherits {@link BasePhaseAgent#act()} without overriding it. */
    @Agent
    @ApplicationScoped
    public static class InheritingAgent extends BasePhaseAgent {

        @Trigger
        public void onEvent(@Observes InheritEvent event) {
            trace.record(Phase.TRIGGER, "onEvent", event);
        }
    }

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

    public record OverrideEvent(String payload) {}

    /**
     * Overrides {@link BasePhaseAgent#act()} without re-applying {@code @Action}. Annotations
     * are not carried onto an override, so the method is not a phase.
     */
    @Agent
    @ApplicationScoped
    public static class OverridingAgent extends BasePhaseAgent {

        @Trigger
        public void onEvent(@Observes OverrideEvent event) {
            trace.record(Phase.TRIGGER, "onEvent", event);
        }

        @Override
        public void act() {
            // Intentionally unannotated, so it must not run as an @Action phase.
        }
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
