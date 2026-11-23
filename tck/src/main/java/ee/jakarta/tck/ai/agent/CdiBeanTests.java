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
import jakarta.ai.agent.Outcome;
import jakarta.ai.agent.Trigger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
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
                    <class>%s</class>
                </interceptors>
            </beans>
            """.formatted(AgentInterceptorImpl.class.getName());

    @Deployment
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "cdibeans.war")
                .addClasses(
                        ConstructorInjectedAgent.class,
                        ConstructorInjectedEvent.class,
                        InterceptedAgent.class,
                        InterceptedEvent.class,
                        AgentInterceptorBinding.class,
                        AgentInterceptorImpl.class)
                .addAsWebInfResource(new StringAsset(BEANS_XML), "beans.xml");
    }

    @Inject
    Event<ConstructorInjectedEvent> constructorInjectedEvents;

    @Inject
    Event<InterceptedEvent> interceptedEvents;

    @Inject
    ExecutionTraceRecorder trace;

    public record ConstructorInjectedEvent(String payload) {}

    @Agent
    @ApplicationScoped
    public static class ConstructorInjectedAgent {

        private final ExecutionTraceRecorder trace;

        /** Required by CDI for normal-scoped bean proxying; the @Inject constructor is used for actual instantiation. */
        protected ConstructorInjectedAgent() {
            this.trace = null;
        }

        @Inject
        public ConstructorInjectedAgent(ExecutionTraceRecorder trace) {
            this.trace = trace;
        }

        @Trigger
        public void onEvent(@Observes ConstructorInjectedEvent event) {
            trace.record(Phase.TRIGGER, "onEvent", event);
        }
    }

    /**
     * An agent that receives its dependencies through an {@code @Inject} constructor is a valid
     * CDI managed bean. Its {@code @Trigger} fires with the injected recorder, so the container
     * created it through that constructor.
     *
     * <p>Specification section {@code agents-as-cdi-beans}.
     */
    @Test
    public void constructorInjectedAgentIsDiscoveredAndManaged() {
        trace.reset();
        constructorInjectedEvents.fire(new ConstructorInjectedEvent("test"));

        assertEquals(List.of(Phase.TRIGGER), trace.phases());
    }

    @InterceptorBinding
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    public @interface AgentInterceptorBinding {}

    // No @Priority. The interceptor is enabled through the <interceptors> element of BEANS_XML.
    @AgentInterceptorBinding
    @Interceptor
    public static class AgentInterceptorImpl {

        @Inject
        ExecutionTraceRecorder trace;

        @AroundInvoke
        public Object intercept(InvocationContext ctx) throws Exception {
            trace.record(Phase.ACTION, "interceptorBefore", ctx.getMethod().getName());
            Object result = ctx.proceed();
            trace.record(Phase.ACTION, "interceptorAfter", ctx.getMethod().getName());
            return result;
        }
    }

    public record InterceptedEvent(String payload) {}

    @Agent
    @ApplicationScoped
    public static class InterceptedAgent {

        @Inject
        ExecutionTraceRecorder trace;

        @Trigger
        public void onEvent(@Observes InterceptedEvent event) {
            trace.record(Phase.TRIGGER, "onEvent", event);
        }

        @Action
        @AgentInterceptorBinding
        public void process(InterceptedEvent event) {
            trace.record(Phase.ACTION, "process", event);
        }

        // Bound to the @Outcome phase too, which shows that interception applies to every
        // phase the implementation dispatches, not only @Action.
        @Outcome
        @AgentInterceptorBinding
        public void finish(InterceptedEvent event) {
            trace.record(Phase.OUTCOME, "finish", event);
        }
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

        List<String> names = trace.methodNames();
        assertEquals(
                List.of(
                        "onEvent",
                        "interceptorBefore",
                        "process",
                        "interceptorAfter",
                        "interceptorBefore",
                        "finish",
                        "interceptorAfter"),
                names);
    }
}
