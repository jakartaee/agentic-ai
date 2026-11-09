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

import ee.jakarta.tck.ai.agent.core.behavior.agents.topologyflex.ImplicitObservesAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.topologyflex.ImplicitObservesEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.topologyflex.NoDecisionAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.topologyflex.NoDecisionEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.topologyflex.NoOutcomeAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.topologyflex.NoOutcomeEvent;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder.Phase;
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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@ExtendWith(ArquillianExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TopologyFlexTests {

    @Deployment
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "topologyflex.war")
                .addClasses(NoDecisionAgent.class, NoDecisionEvent.class,
                            NoOutcomeAgent.class,  NoOutcomeEvent.class,
                            ImplicitObservesAgent.class, ImplicitObservesEvent.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject Event<NoDecisionEvent> noDecisionEvents;
    @Inject Event<NoOutcomeEvent>  noOutcomeEvents;
    @Inject Event<ImplicitObservesEvent> implicitObservesEvents;
    @Inject ExecutionTraceRecorder trace;
    /**
     * Workflow without {@code @Decision} executes T→A→O without error.
     *
     * <p>Specification section {@code agent-lifecycle}.
     */
    @Test
    public void workflowWithoutDecisionExecutesActions() {
        trace.reset();
        noDecisionEvents.fire(new NoDecisionEvent("test"));
        assertEquals(List.of(Phase.TRIGGER, Phase.ACTION, Phase.OUTCOME), trace.phases());
    }
    /**
     * Workflow without {@code @Outcome} completes gracefully after {@code @Action}.
     *
     * <p>Specification section {@code agent-lifecycle}.
     */
    @Test
    public void workflowWithoutOutcomeCompletesGracefully() {
        trace.reset();
        noOutcomeEvents.fire(new NoOutcomeEvent("test"));
        assertEquals(List.of(Phase.TRIGGER, Phase.DECISION, Phase.ACTION), trace.phases());
    }
    /**
     * A {@code @Trigger} whose event parameter carries no {@code @Observes} is still invoked by
     * the triggering event and runs the full T→A→O workflow.
     *
     * <p>Specification section {@code agent-lifecycle}.
     */
    @Test
    public void triggerWithoutObservesRunsWorkflow() {
        trace.reset();
        implicitObservesEvents.fire(new ImplicitObservesEvent("test"));
        assertEquals(List.of(Phase.TRIGGER, Phase.ACTION, Phase.OUTCOME), trace.phases());
        assertEquals("test", assertInstanceOf(ImplicitObservesEvent.class, trace.entries().get(0).args()[0]).payload());
    }
}
