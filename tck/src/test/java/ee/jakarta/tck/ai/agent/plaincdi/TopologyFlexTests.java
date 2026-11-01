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
package ee.jakarta.tck.ai.agent.plaincdi;

import ee.jakarta.tck.ai.agent.core.behavior.agents.topologyflex.NoDecisionEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.topologyflex.NoOutcomeEvent;
import ee.jakarta.tck.ai.agent.framework.stub.LargeLanguageModelStub;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder.Phase;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The parts of {@link ee.jakarta.tck.ai.agent.core.behavior.TopologyFlexTests} that plain CDI satisfies: the
 * archive deploys and the {@code @Trigger} fires. Run only in the
 * specification repository on Weld Embedded; not part of the TCK.
 */
@ExtendWith(ArquillianExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TopologyFlexTests {

    @Deployment
    public static Archive<?> createDeployment() {
        return ee.jakarta.tck.ai.agent.core.behavior.TopologyFlexTests.createDeployment();
    }

    @Inject Event<NoDecisionEvent> noDecisionEvents;
    @Inject Event<NoOutcomeEvent>  noOutcomeEvents;
    @Inject LargeLanguageModelStub llm;
    @Inject ExecutionTraceRecorder trace;

    @Test
    public void noDecisionAgentTriggerObserved() {
        llm.reset();
        trace.reset();
        noDecisionEvents.fire(new NoDecisionEvent("test"));
        assertEquals(List.of(Phase.TRIGGER), trace.phases());
    }

    @Test
    public void noOutcomeAgentTriggerObserved() {
        llm.reset();
        trace.reset();
        noOutcomeEvents.fire(new NoOutcomeEvent("test"));
        assertEquals(List.of(Phase.TRIGGER), trace.phases());
    }
}
