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

import ee.jakarta.tck.ai.agent.core.integration.GreetEvent;
import ee.jakarta.tck.ai.agent.framework.stub.LargeLanguageModelStub;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder.Phase;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The parts of {@link ee.jakarta.tck.ai.agent.core.integration.AgentSmokeTest} that plain CDI satisfies: the
 * archive deploys and the {@code @Trigger} fires. Run only in the
 * specification repository on Weld Embedded; not part of the TCK.
 */
@ExtendWith(ArquillianExtension.class)
public class AgentSmokeTest {

    @Deployment
    public static Archive<?> createDeployment() {
        return ee.jakarta.tck.ai.agent.core.integration.AgentSmokeTest.createDeployment();
    }

    @Inject Event<GreetEvent> events;
    @Inject LargeLanguageModelStub llm;
    @Inject ExecutionTraceRecorder trace;

    @BeforeEach
    public void setUp() {
        llm.reset();
        trace.reset();
    }

    @Test
    public void deploymentTriggersAgentObserver() {
        events.fire(new GreetEvent("Jakarta AI"));

        assertEquals(List.of(Phase.TRIGGER), trace.phases());

        ExecutionTraceRecorder.TraceEntry entry = trace.entries().get(0);
        assertEquals("onGreet", entry.methodName(), "Trigger method name");
        assertTrue(entry.args()[0] instanceof GreetEvent, "Trigger should receive the event");
        assertEquals("Jakarta AI", ((GreetEvent) entry.args()[0]).getName(), "Event payload preserved");
    }
}
