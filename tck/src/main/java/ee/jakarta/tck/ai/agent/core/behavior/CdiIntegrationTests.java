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

import ee.jakarta.tck.ai.agent.core.behavior.agents.cdi.AgentInterceptorBinding;
import ee.jakarta.tck.ai.agent.core.behavior.agents.cdi.AgentInterceptorImpl;
import ee.jakarta.tck.ai.agent.core.behavior.agents.cdi.ConstructorInjectedAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.cdi.ConstructorInjectedEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.cdi.InnerAgentContainer;
import ee.jakarta.tck.ai.agent.core.behavior.agents.cdi.InnerAgentEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.cdi.InterceptedAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.cdi.InterceptedEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.cdi.ResolutionCdiBean;
import ee.jakarta.tck.ai.agent.core.behavior.agents.cdi.ResolutionOrderAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.cdi.ResolutionOrderEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.cdi.SingletonCdiAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.cdi.SingletonCdiEvent;
import ee.jakarta.tck.ai.agent.framework.junit.anno.Assertion;
import ee.jakarta.tck.ai.agent.framework.junit.anno.Deployed;
import ee.jakarta.tck.ai.agent.framework.junit.anno.RequiresEngine;
import ee.jakarta.tck.ai.agent.framework.junit.anno.RequiresNoEngine;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder.Phase;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder.TraceEntry;
import jakarta.ai.agent.LargeLanguageModel;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.TestInstance;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Deployed
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CdiIntegrationTests {

    // Interceptor enabled here (NOT via @Priority — see constraint #5 in steps.md).
    private static final String BEANS_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <beans xmlns="https://jakarta.ee/xml/ns/jakartaee"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee
                       https://jakarta.ee/xml/ns/jakartaee/beans_4_0.xsd"
                   version="4.0">
                <interceptors>
                    <class>ee.jakarta.tck.ai.agent.core.behavior.agents.cdi.AgentInterceptorImpl</class>
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
    @Inject SingletonCdiAgent               singletonAgent;

    // -------------------------------------------------------------------------
    // CDI-only tests (no RI required)
    // -------------------------------------------------------------------------

    @Assertion(id = "AGENTICAI-CDI-BHV-002",
               section = "Architecture, Convention over Configuration",
               strategy = "A static nested class annotated @Agent @ApplicationScoped is discovered as a CDI "
                        + "managed bean and its @Trigger observer fires when the triggering event is fired")
    public void staticInnerAgentIsDiscoveredAsCdiBean() {
        trace.reset();
        innerAgentEvents.fire(new InnerAgentEvent("test"));
        assertThat(trace.phases()).containsExactly(Phase.TRIGGER);
    }

    @Assertion(id = "AGENTICAI-CDI-BHV-002",
               section = "Architecture, Convention over Configuration",
               strategy = "An @Agent bean using @Inject constructor injection (no no-args constructor) is a valid "
                        + "CDI managed bean; the @Trigger fires, proving the container created it via its constructor")
    public void constructorInjectedAgentIsDiscoveredAndManaged() {
        trace.reset();
        constructorInjectedEvents.fire(new ConstructorInjectedEvent("test"));
        assertThat(trace.phases()).containsExactly(Phase.TRIGGER);
    }

    @Assertion(id = "AGENTICAI-CDI-BHV-002",
               section = "Architecture, Convention over Configuration",
               strategy = "An @ApplicationScoped @Agent is a first-class CDI bean: the container injects a "
                        + "client proxy (a generated subclass), not the raw instance. Proven by the injected "
                        + "reference being an instance of the agent type but having a different runtime class")
    public void agentIsInjectedAsCdiProxy() {
        // Normal-scoped beans are always client-proxied per the CDI spec, so the proxy
        // is a subclass with a different Class object. Portable across CDI implementations.
        assertThat(singletonAgent).isInstanceOf(SingletonCdiAgent.class);
        assertThat(singletonAgent.getClass()).isNotEqualTo(SingletonCdiAgent.class);
    }

    @RequiresNoEngine
    @Assertion(id = "AGENTICAI-CDI-BHV-003-PRECONDITION",
               section = "CDI Integration, Lifecycle",
               strategy = "SingletonCdiAgent @Trigger fires via CDI; confirms the bean is instantiated and managed")
    public void singletonAgentTriggerObserved() {
        trace.reset();
        singletonEvents.fire(new SingletonCdiEvent("test"));
        assertThat(trace.phases()).containsExactly(Phase.TRIGGER);
    }

    @Assertion(id = "AGENTICAI-CDI-BHV-003",
               section = "CDI Integration, Lifecycle",
               strategy = "@PostConstruct is invoked exactly once for an @ApplicationScoped @Agent regardless of "
                        + "how many workflow events are fired; the static instance counter stays 1")
    public void applicationScopedAgentPostConstructCalledOnce() {
        trace.reset();
        singletonEvents.fire(new SingletonCdiEvent("first"));
        singletonEvents.fire(new SingletonCdiEvent("second"));
        assertThat(SingletonCdiAgent.getInstanceCount()).isEqualTo(1);
    }

    @Assertion(id = "AGENTICAI-CDI-BHV-004",
               section = "CDI Integration, Scopes",
               strategy = "An @ApplicationScoped @Agent reuses the same bean instance across sequential workflow "
                        + "events — both @Trigger invocations record the same instanceId in the trace")
    public void applicationScopedAgentSharesInstanceAcrossWorkflows() {
        trace.reset();
        singletonEvents.fire(new SingletonCdiEvent("first"));
        int id1 = (int) trace.entries().get(0).args()[1];

        trace.reset();
        singletonEvents.fire(new SingletonCdiEvent("second"));
        int id2 = (int) trace.entries().get(0).args()[1];

        assertThat(id1).isEqualTo(id2);
    }

    @RequiresNoEngine
    @Assertion(id = "AGENTICAI-CDI-BHV-006-PRECONDITION",
               section = "CDI Integration, Interceptors",
               strategy = "InterceptedAgent @Trigger fires via CDI; confirms the agent and its enabled interceptor "
                        + "deploy without error")
    public void interceptedAgentTriggerObserved() {
        trace.reset();
        interceptedEvents.fire(new InterceptedEvent("test"));
        assertThat(trace.phases()).containsExactly(Phase.TRIGGER);
    }

    @RequiresNoEngine
    @Assertion(id = "AGENTICAI-CDI-BHV-005-PRECONDITION",
               section = "Workflow Orchestration, Parameter Injection",
               strategy = "ResolutionOrderAgent @Trigger fires via CDI; confirms the agent and its CDI-bean "
                        + "collaborator (ResolutionCdiBean) deploy without resolution errors")
    public void resolutionOrderAgentTriggerObserved() {
        trace.reset();
        resolutionOrderEvents.fire(new ResolutionOrderEvent("test"));
        assertThat(trace.phases()).containsExactly(Phase.TRIGGER);
    }

    // -------------------------------------------------------------------------
    // RI-required tests
    // -------------------------------------------------------------------------

    @RequiresEngine
    @Assertion(id = "AGENTICAI-CDI-BHV-004",
               section = "CDI Integration, Scopes",
               strategy = "An @ApplicationScoped @Agent uses the same bean instance across all phases of a "
                        + "workflow — @Trigger, @Action, @Outcome all record the same instanceId")
    public void applicationScopedAgentUseSameInstanceAcrossAllPhases() {
        trace.reset();
        singletonEvents.fire(new SingletonCdiEvent("test"));
        assertThat(trace.phases())
                .containsExactly(Phase.TRIGGER, Phase.ACTION, Phase.OUTCOME);
        int triggerId = (int) trace.entries().get(0).args()[1];
        int actionId  = (int) trace.entries().get(1).args()[1];
        int outcomeId = (int) trace.entries().get(2).args()[1];
        assertThat(actionId).isEqualTo(triggerId);
        assertThat(outcomeId).isEqualTo(triggerId);
    }

    @RequiresEngine
    @Assertion(id = "AGENTICAI-CDI-BHV-005",
               section = "Workflow Orchestration, Parameter Injection",
               strategy = "The RI resolves method parameters in priority order: (1) triggering event, (3) "
                        + "LargeLanguageModel, (4) CDI beans — ResolutionOrderAgent @Action args are "
                        + "[0]=event, [1]=LLM, [2]=ResolutionCdiBean")
    public void parameterResolutionFollowsDefinedPriority() {
        trace.reset();
        resolutionOrderEvents.fire(new ResolutionOrderEvent("test"));
        assertThat(trace.phases())
                .containsExactly(Phase.TRIGGER, Phase.DECISION, Phase.ACTION, Phase.OUTCOME);
        var actionEntry = trace.entries().stream()
                .filter(e -> e.phase() == Phase.ACTION && e.methodName().equals("process"))
                .findFirst().orElseThrow();
        assertThat(actionEntry.args()[0]).isInstanceOf(ResolutionOrderEvent.class);
        assertThat(actionEntry.args()[1]).isInstanceOf(LargeLanguageModel.class);
        assertThat(actionEntry.args()[2]).isInstanceOf(ResolutionCdiBean.class);
    }

    @RequiresEngine
    @Assertion(id = "AGENTICAI-CDI-BHV-006",
               section = "CDI Integration, Interceptors",
               strategy = "A CDI interceptor bound via a custom @InterceptorBinding executes before and after the "
                        + "method body for EVERY business-method phase the RI dispatches through the CDI proxy — "
                        + "verified by binding the interceptor to both @Action (process) and @Outcome (finish) and "
                        + "asserting the interceptor wraps each. Asserts on method-name order because the interceptor "
                        + "marker uses a fixed phase label")
    public void cdiInterceptorExecutesAroundActionAndOutcome() {
        trace.reset();
        interceptedEvents.fire(new InterceptedEvent("test"));
        List<String> names = trace.entries().stream().map(TraceEntry::methodName).toList();
        assertThat(names).containsExactly(
                "onEvent",
                "interceptorBefore", "process",  "interceptorAfter",
                "interceptorBefore", "finish",   "interceptorAfter");
    }

    @RequiresEngine
    @Assertion(id = "AGENTICAI-CDI-BHV-007",
               section = "Workflow Orchestration",
               strategy = "The triggering event object is available as a method parameter in @Decision, @Action, "
                        + "and @Outcome — all three downstream phases receive it as args()[0]")
    public void triggeringEventIsAvailableInAllDownstreamPhases() {
        trace.reset();
        resolutionOrderEvents.fire(new ResolutionOrderEvent("event-payload"));
        assertThat(trace.phases())
                .containsExactly(Phase.TRIGGER, Phase.DECISION, Phase.ACTION, Phase.OUTCOME);
        trace.entries().stream()
                .filter(e -> e.phase() != Phase.TRIGGER)
                .forEach(e -> assertThat(e.args()[0])
                        .isInstanceOf(ResolutionOrderEvent.class)
                        .extracting(o -> ((ResolutionOrderEvent) o).payload())
                        .isEqualTo("event-payload"));
    }
}
