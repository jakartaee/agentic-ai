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

import ee.jakarta.tck.ai.agent.core.behavior.agents.contextinjection.ContextInjectionAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.contextinjection.ContextInjectionEvent;
import ee.jakarta.tck.ai.agent.framework.junit.anno.Assertion;
import ee.jakarta.tck.ai.agent.framework.junit.anno.Deployed;
import ee.jakarta.tck.ai.agent.framework.junit.anno.RequiresImplementation;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@Deployed
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ContextInjectionTests {

    @Deployment
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "contextinjection.war")
                .addClasses(ContextInjectionAgent.class, ContextInjectionEvent.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject Event<ContextInjectionEvent> events;
    @Inject ExecutionTraceRecorder trace;

    @Assertion(id = "AGENTICAI-CTX-002",
               section = "agent-lifecycle",
               strategy = "Triggering event payload is accessible in @Trigger and preserved in the trace")
    public void eventPayloadIsPreservedInTrigger() {
        trace.reset();
        events.fire(new ContextInjectionEvent("my-payload"));

        ContextInjectionEvent recorded = (ContextInjectionEvent) trace.entries().get(0).args()[0];
        assertEquals("my-payload", recorded.payload());
    }

    @RequiresImplementation
    @Assertion(id = "AGENTICAI-CTX-003",
               section = "parameter-resolution",
               strategy = "Triggering event is available for injection in @Action")
    public void eventIsInjectableInAction() {
        trace.reset();
        events.fire(new ContextInjectionEvent("payload"));
        assertEquals(List.of(Phase.TRIGGER, Phase.ACTION, Phase.OUTCOME), trace.phases());
        assertInstanceOf(ContextInjectionEvent.class, trace.entries().stream()
                .filter(e -> e.phase() == Phase.ACTION)
                .findFirst()
                .orElseThrow()
                .args()[0]);
    }

    @RequiresImplementation
    @Assertion(id = "AGENTICAI-CTX-004",
               section = "parameter-resolution",
               strategy = "LargeLanguageModel is injectable as a direct method parameter of @Action and is a "
                        + "working facade: its echoed response is the rendered prompt")
    public void llmIsInjectedInAction() {
        trace.reset();
        events.fire(new ContextInjectionEvent("payload"));
        assertEquals(List.of(Phase.TRIGGER, Phase.ACTION, Phase.OUTCOME), trace.phases());
        assertEquals("classify \"payload\"", trace.entries().get(1).args()[1]);
    }
}
