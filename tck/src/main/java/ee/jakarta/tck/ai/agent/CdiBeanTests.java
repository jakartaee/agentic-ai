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

import ee.jakarta.tck.ai.agent.agents.cdibeans.AgentInterceptorBinding;
import ee.jakarta.tck.ai.agent.agents.cdibeans.AgentInterceptorImpl;
import ee.jakarta.tck.ai.agent.agents.cdibeans.ConstructorInjectedAgent;
import ee.jakarta.tck.ai.agent.agents.cdibeans.ConstructorInjectedEvent;
import ee.jakarta.tck.ai.agent.agents.cdibeans.InnerAgentContainer;
import ee.jakarta.tck.ai.agent.agents.cdibeans.InnerAgentEvent;
import ee.jakarta.tck.ai.agent.agents.cdibeans.InterceptedAgent;
import ee.jakarta.tck.ai.agent.agents.cdibeans.InterceptedEvent;
import ee.jakarta.tck.ai.agent.framework.ExecutionTraceRecorder;
import ee.jakarta.tck.ai.agent.framework.ExecutionTraceRecorder.Phase;
import ee.jakarta.tck.ai.agent.framework.ExecutionTraceRecorder.TraceEntry;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Agents as CDI beans (specification section <em>Agents as CDI Beans</em> and
 * <em>Interceptors</em>). An agent is discovered and managed like any other bean, may use
 * constructor injection or be a static nested class, and CDI interceptors apply to its phases.
 *
 * <p>Uses {@code @TestInstance(PER_CLASS)} with inline {@code trace.reset()} because
 * {@code @BeforeEach} does not run reliably between methods of an Arquillian test
 * class inside the container.</p>
 */
@ExtendWith(ArquillianExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CdiBeanTests {

    // AgentInterceptorImpl carries no @Priority. Enabling it both ways would enable it twice.
    private static final String BEANS_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <beans xmlns="https://jakarta.ee/xml/ns/jakartaee"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee
                       https://jakarta.ee/xml/ns/jakartaee/beans_4_0.xsd"
                   version="4.0">
                <interceptors>
                    <class>ee.jakarta.tck.ai.agent.agents.cdibeans.AgentInterceptorImpl</class>
                </interceptors>
            </beans>
            """;

    @Deployment
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "cdibeans.war")
                .addClasses(
                        InnerAgentContainer.class,       InnerAgentContainer.InnerAgent.class, InnerAgentEvent.class,
                        ConstructorInjectedAgent.class,  ConstructorInjectedEvent.class,
                        InterceptedAgent.class,          InterceptedEvent.class,
                        AgentInterceptorBinding.class,   AgentInterceptorImpl.class
                )
                .addAsWebInfResource(new StringAsset(BEANS_XML), "beans.xml");
    }

    @Inject Event<InnerAgentEvent>          innerAgentEvents;
    @Inject Event<ConstructorInjectedEvent> constructorInjectedEvents;
    @Inject Event<InterceptedEvent>         interceptedEvents;
    @Inject ExecutionTraceRecorder          trace;

    /**
     * A static nested class annotated {@code @Agent} is discovered as a CDI managed bean, and its
     * {@code @Trigger} fires when the triggering event is fired.
     *
     * <p>Specification section {@code agents-as-cdi-beans}.
     */
    @Test
    public void staticNestedAgentIsDiscoveredAsCdiBean() {
        trace.reset();
        innerAgentEvents.fire(new InnerAgentEvent("test"));
        assertEquals(List.of(Phase.TRIGGER), trace.phases());
    }

    /**
     * An agent using {@code @Inject} constructor injection, with no no-args constructor, is a
     * valid CDI managed bean. Its {@code @Trigger} fires, so the container created it through
     * its constructor.
     *
     * <p>Specification section {@code agents-as-cdi-beans}.
     */
    @Test
    public void constructorInjectedAgentIsDiscoveredAndManaged() {
        trace.reset();
        constructorInjectedEvents.fire(new ConstructorInjectedEvent("test"));
        assertEquals(List.of(Phase.TRIGGER), trace.phases());
    }

    /**
     * A CDI interceptor bound through a custom {@code @InterceptorBinding} runs before and after
     * every phase it is bound to, here the {@code @Action} and the {@code @Outcome}.
     *
     * <p>Specification section {@code interceptors}.
     */
    @Test
    public void cdiInterceptorExecutesAroundActionAndOutcome() {
        trace.reset();
        interceptedEvents.fire(new InterceptedEvent("test"));
        List<String> names = trace.entries().stream().map(TraceEntry::methodName).toList();
        assertEquals(List.of("onEvent",
                "interceptorBefore", "process", "interceptorAfter",
                "interceptorBefore", "finish",  "interceptorAfter"), names);
    }
}
