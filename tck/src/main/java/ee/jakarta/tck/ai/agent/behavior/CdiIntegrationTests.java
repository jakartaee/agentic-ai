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
package ee.jakarta.tck.ai.agent.behavior;

import ee.jakarta.tck.ai.agent.behavior.agents.cdi.AgentInterceptorBinding;
import ee.jakarta.tck.ai.agent.behavior.agents.cdi.AgentInterceptorImpl;
import ee.jakarta.tck.ai.agent.behavior.agents.cdi.ConstructorInjectedAgent;
import ee.jakarta.tck.ai.agent.behavior.agents.cdi.ConstructorInjectedEvent;
import ee.jakarta.tck.ai.agent.behavior.agents.cdi.InnerAgentContainer;
import ee.jakarta.tck.ai.agent.behavior.agents.cdi.InnerAgentEvent;
import ee.jakarta.tck.ai.agent.behavior.agents.cdi.InterceptedAgent;
import ee.jakarta.tck.ai.agent.behavior.agents.cdi.InterceptedEvent;
import ee.jakarta.tck.ai.agent.behavior.agents.cdi.ResolutionCdiBean;
import ee.jakarta.tck.ai.agent.behavior.agents.cdi.ResolutionOrderAgent;
import ee.jakarta.tck.ai.agent.behavior.agents.cdi.ResolutionOrderEvent;
import ee.jakarta.tck.ai.agent.behavior.agents.cdi.SingletonCdiAgent;
import ee.jakarta.tck.ai.agent.behavior.agents.cdi.SingletonCdiEvent;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder.Phase;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder.TraceEntry;
import jakarta.ai.agent.LargeLanguageModel;
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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@ExtendWith(ArquillianExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CdiIntegrationTests {

    // AgentInterceptorImpl carries no @Priority. Enabling it both ways would enable it twice.
    private static final String BEANS_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <beans xmlns="https://jakarta.ee/xml/ns/jakartaee"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee
                       https://jakarta.ee/xml/ns/jakartaee/beans_4_0.xsd"
                   version="4.0">
                <interceptors>
                    <class>ee.jakarta.tck.ai.agent.behavior.agents.cdi.AgentInterceptorImpl</class>
                </interceptors>
            </beans>
            """;

    @Deployment
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "cdiintegration.war")
                .addClasses(
                        SingletonCdiAgent.class,         SingletonCdiEvent.class,
                        ConstructorInjectedAgent.class,  ConstructorInjectedEvent.class,
                        InterceptedAgent.class,          InterceptedEvent.class,
                        AgentInterceptorBinding.class,   AgentInterceptorImpl.class,
                        ResolutionOrderAgent.class,      ResolutionOrderEvent.class,
                        ResolutionCdiBean.class,
                        InnerAgentContainer.class,       InnerAgentContainer.InnerAgent.class,
                        InnerAgentEvent.class
                )
                .addAsWebInfResource(new StringAsset(BEANS_XML), "beans.xml");
    }

    @Inject Event<SingletonCdiEvent>        singletonEvents;
    @Inject Event<ConstructorInjectedEvent> constructorInjectedEvents;
    @Inject Event<InterceptedEvent>         interceptedEvents;
    @Inject Event<ResolutionOrderEvent>     resolutionOrderEvents;
    @Inject Event<InnerAgentEvent>          innerAgentEvents;
    @Inject ExecutionTraceRecorder          trace;

    /**
     * A static nested class annotated {@code @Agent} {@code @ApplicationScoped} is discovered as a
     * CDI managed bean and its {@code @Trigger} observer fires when the triggering event is fired.
     *
     * <p>Specification section {@code cdi-integration}.
     */
    @Test
    public void staticInnerAgentIsDiscoveredAsCdiBean() {
        trace.reset();
        innerAgentEvents.fire(new InnerAgentEvent("test"));
        assertEquals(List.of(Phase.TRIGGER), trace.phases());
    }

    /**
     * An {@code @Agent} bean using {@code @Inject} constructor injection (no no-args constructor)
     * is a valid CDI managed bean; the {@code @Trigger} fires, proving the container created it
     * via its constructor.
     *
     * <p>Specification section {@code cdi-integration}.
     */
    @Test
    public void constructorInjectedAgentIsDiscoveredAndManaged() {
        trace.reset();
        constructorInjectedEvents.fire(new ConstructorInjectedEvent("test"));
        assertEquals(List.of(Phase.TRIGGER), trace.phases());
    }

    /**
     * {@code @PostConstruct} is invoked exactly once for an {@code @ApplicationScoped} {@code
     * @Agent} regardless of how many workflow events are fired; the static instance counter stays
     * 1.
     *
     * <p>Specification section {@code cdi-integration}.
     */
    @Test
    public void applicationScopedAgentPostConstructCalledOnce() {
        trace.reset();
        singletonEvents.fire(new SingletonCdiEvent("first"));
        singletonEvents.fire(new SingletonCdiEvent("second"));
        assertEquals(1, SingletonCdiAgent.getInstanceCount());
    }

    /**
     * An {@code @ApplicationScoped} {@code @Agent} reuses the same bean instance across sequential
     * workflow events — both {@code @Trigger} invocations record the same instanceId in the trace.
     *
     * <p>Specification section {@code cdi-integration}.
     */
    @Test
    public void applicationScopedAgentSharesInstanceAcrossWorkflows() {
        trace.reset();
        singletonEvents.fire(new SingletonCdiEvent("first"));
        int id1 = (int) trace.entries().get(0).args()[1];

        trace.reset();
        singletonEvents.fire(new SingletonCdiEvent("second"));
        int id2 = (int) trace.entries().get(0).args()[1];

        assertEquals(id2, id1);
    }

    // -------------------------------------------------------------------------
    // Implementation-required tests
    // -------------------------------------------------------------------------
    /**
     * An {@code @ApplicationScoped} {@code @Agent} uses the same bean instance across all phases
     * of a workflow — {@code @Trigger}, {@code @Action}, {@code @Outcome} all record the same
     * instanceId.
     *
     * <p>Specification section {@code cdi-integration}.
     */
    @Test
    public void applicationScopedAgentUseSameInstanceAcrossAllPhases() {
        trace.reset();
        singletonEvents.fire(new SingletonCdiEvent("test"));
        assertEquals(List.of(Phase.TRIGGER, Phase.ACTION, Phase.OUTCOME), trace.phases());
        int triggerId = (int) trace.entries().get(0).args()[1];
        int actionId  = (int) trace.entries().get(1).args()[1];
        int outcomeId = (int) trace.entries().get(2).args()[1];
        assertEquals(triggerId, actionId);
        assertEquals(triggerId, outcomeId);
    }
    /**
     * The compatible implementation resolves method parameters in priority order: (1) triggering
     * event, (3) LargeLanguageModel, (5) CDI beans — ResolutionOrderAgent {@code @Action} args are
     * [0]=event, [1]=LLM, [2]=ResolutionCdiBean.
     *
     * <p>Specification section {@code parameter-resolution}.
     */
    @Test
    public void parameterResolutionFollowsDefinedPriority() {
        trace.reset();
        resolutionOrderEvents.fire(new ResolutionOrderEvent("test"));
        assertEquals(List.of(Phase.TRIGGER, Phase.DECISION, Phase.ACTION, Phase.OUTCOME), trace.phases());
        var actionEntry = trace.entries().stream()
                .filter(e -> e.phase() == Phase.ACTION && e.methodName().equals("process"))
                .findFirst().orElseThrow();
        assertInstanceOf(ResolutionOrderEvent.class, actionEntry.args()[0]);
        assertInstanceOf(LargeLanguageModel.class, actionEntry.args()[1]);
        assertInstanceOf(ResolutionCdiBean.class, actionEntry.args()[2]);
    }
    /**
     * A CDI interceptor bound via a custom {@code @InterceptorBinding} executes before and after
     * the method body for EVERY business-method phase the compatible implementation dispatches
     * through the CDI proxy — verified by binding the interceptor to both {@code @Action}
     * (process) and {@code @Outcome} (finish) and asserting the interceptor wraps each. Asserts on
     * method-name order because the interceptor marker uses a fixed phase label.
     *
     * <p>Specification section {@code cdi-integration}.
     */
    @Test
    public void cdiInterceptorExecutesAroundActionAndOutcome() {
        trace.reset();
        interceptedEvents.fire(new InterceptedEvent("test"));
        List<String> names = trace.entries().stream().map(TraceEntry::methodName).toList();
        assertEquals(List.of("onEvent",
                "interceptorBefore", "process",  "interceptorAfter",
                "interceptorBefore", "finish",   "interceptorAfter"), names);
    }
    /**
     * The triggering event object is available as a method parameter in {@code @Decision}, {@code
     * @Action}, and {@code @Outcome} — all three downstream phases receive it as args()[0].
     *
     * <p>Specification section {@code agent-lifecycle}.
     */
    @Test
    public void triggeringEventIsAvailableInAllDownstreamPhases() {
        trace.reset();
        resolutionOrderEvents.fire(new ResolutionOrderEvent("event-payload"));
        assertEquals(List.of(Phase.TRIGGER, Phase.DECISION, Phase.ACTION, Phase.OUTCOME), trace.phases());
        trace.entries().stream()
                .filter(e -> e.phase() != Phase.TRIGGER)
                .forEach(e -> assertEquals("event-payload",
                        assertInstanceOf(ResolutionOrderEvent.class, e.args()[0]).payload()));
    }
}
