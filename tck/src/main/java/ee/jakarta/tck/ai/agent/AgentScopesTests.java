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
import ee.jakarta.tck.ai.agent.framework.LifecycleRecorder;
import jakarta.ai.agent.Action;
import jakarta.ai.agent.Agent;
import jakarta.ai.agent.Decision;
import jakarta.ai.agent.Outcome;
import jakarta.ai.agent.Trigger;
import jakarta.ai.agent.WorkflowScoped;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Agent scopes and lifecycle callbacks (specification sections <em>Agent Scopes</em> and
 * <em>Lifecycle Callbacks</em>). {@code @WorkflowScoped} is the default and gives each workflow
 * its own instance, destroyed when the workflow ends however it ends; {@code @ApplicationScoped}
 * shares one instance across workflows and phases.
 *
 * <p>Uses {@code @TestInstance(PER_CLASS)} with inline {@code trace.reset()} because
 * {@code @BeforeEach} does not run reliably between methods of an Arquillian test class inside
 * the container.
 */
@ExtendWith(ArquillianExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AgentScopesTests {

    @Deployment
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "agentscopes.war")
                .addClasses(
                        ScopeDefaultAgent.class,
                        ScopeDefaultEvent.class,
                        LifecycleSpyAgent.class,
                        LifecycleSpyEvent.class,
                        DecisionStopSpyAgent.class,
                        DecisionStopEvent.class,
                        SingletonCdiAgent.class,
                        SingletonCdiEvent.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject
    Event<ScopeDefaultEvent> scopeDefaultEvents;

    @Inject
    Event<LifecycleSpyEvent> lifecycleSpyEvents;

    @Inject
    Event<DecisionStopEvent> decisionStopEvents;

    @Inject
    Event<SingletonCdiEvent> singletonEvents;

    @Inject
    ExecutionTraceRecorder trace;

    @Inject
    LifecycleRecorder lifecycle;

    @Inject
    BeanManager beanManager;

    public record ScopeDefaultEvent(String payload) {}

    /** Declares no scope, so the {@code @WorkflowScoped} default declared by {@code @Agent} applies. */
    @Agent
    public static class ScopeDefaultAgent {

        @Inject
        ExecutionTraceRecorder trace;

        @Inject
        LifecycleRecorder lifecycle;

        private String instanceId;

        @PostConstruct
        void init() {
            instanceId = lifecycle.postConstruct(ScopeDefaultAgent.class);
        }

        @Trigger
        public void onEvent(@Observes ScopeDefaultEvent event) {
            trace.record(Phase.TRIGGER, "onEvent", event, instanceId);
        }

        @Action
        public void process(ScopeDefaultEvent event) {
            trace.record(Phase.ACTION, "process", event, instanceId);
        }

        @Outcome
        public void finish(ScopeDefaultEvent event) {
            trace.record(Phase.OUTCOME, "finish", event, instanceId);
        }
    }

    /**
     * An agent with no explicit scope is discovered as a CDI bean in an archive whose
     * {@code beans.xml} is empty, and is {@code @WorkflowScoped}. One instance serves every phase
     * of a workflow, and each workflow gets a distinct instance.
     *
     * <p>Specification section {@code agent-semantics}.
     */
    @Test
    public void defaultScopeIsWorkflowScopedWithOneInstancePerWorkflow() {
        Set<Bean<?>> beans = beanManager.getBeans(ScopeDefaultAgent.class);
        assertEquals(1, beans.size());
        Bean<?> bean = beanManager.resolve(beans);
        assertNotNull(bean);
        assertEquals(WorkflowScoped.class, bean.getScope());

        lifecycle.reset(ScopeDefaultAgent.class);
        trace.reset();
        scopeDefaultEvents.fire(new ScopeDefaultEvent("workflow-1"));
        scopeDefaultEvents.fire(new ScopeDefaultEvent("workflow-2"));

        assertEquals(
                List.of(Phase.TRIGGER, Phase.ACTION, Phase.OUTCOME, Phase.TRIGGER, Phase.ACTION, Phase.OUTCOME),
                trace.phases());

        List<String> ids = lifecycle.constructed(ScopeDefaultAgent.class);
        assertEquals(2, ids.size());
        assertNotEquals(ids.get(1), ids.get(0));

        List<String> phaseInstanceIds =
                trace.entries().stream().map(entry -> (String) entry.args()[1]).toList();
        assertEquals(List.of(ids.get(0), ids.get(0), ids.get(0), ids.get(1), ids.get(1), ids.get(1)), phaseInstanceIds);
    }

    public record LifecycleSpyEvent(String payload) {}

    @Agent(name = "lifecycleSpy")
    @WorkflowScoped
    public static class LifecycleSpyAgent {

        // Static so the failure-path test can set it without injecting this @WorkflowScoped
        // bean, which has no active context in the test method.
        private static volatile boolean failInAction = false;

        @Inject
        ExecutionTraceRecorder trace;

        @Inject
        LifecycleRecorder lifecycle;

        private String instanceId;

        public static void setFailInAction(boolean value) {
            failInAction = value;
        }

        @PostConstruct
        void init() {
            instanceId = lifecycle.postConstruct(LifecycleSpyAgent.class);
        }

        @PreDestroy
        void destroy() {
            lifecycle.preDestroy(LifecycleSpyAgent.class, instanceId);
        }

        @Trigger
        public void onEvent(@Observes LifecycleSpyEvent event) {
            trace.record(Phase.TRIGGER, "onEvent", event, instanceId);
        }

        @Action
        public void process(LifecycleSpyEvent event) {
            trace.record(Phase.ACTION, "process", event, instanceId);
            if (failInAction) {
                throw new IllegalStateException("Simulated @Action failure for @PreDestroy-on-failure test");
            }
        }

        @Outcome
        public void finish(LifecycleSpyEvent event) {
            trace.record(Phase.OUTCOME, "finish", event, instanceId);
        }
    }

    /**
     * An explicitly {@code @WorkflowScoped} agent gets a distinct instance per workflow. Two
     * sequential events yield two different instance ids.
     *
     * <p>Specification section {@code workflowscoped}.
     */
    @Test
    public void workflowScopedAgentHasOneInstancePerWorkflow() {
        lifecycle.reset(LifecycleSpyAgent.class);
        trace.reset();
        lifecycleSpyEvents.fire(new LifecycleSpyEvent("workflow-1"));
        lifecycleSpyEvents.fire(new LifecycleSpyEvent("workflow-2"));

        List<String> ids = lifecycle.constructed(LifecycleSpyAgent.class);
        assertEquals(2, ids.size());
        assertNotEquals(ids.get(1), ids.get(0));
    }

    /**
     * {@code @PostConstruct} is invoked exactly once per {@code @WorkflowScoped} workflow
     * execution.
     *
     * <p>Specification section {@code lifecycle-callbacks}.
     */
    @Test
    public void workflowScopedAgentPostConstructCalledOnce() {
        lifecycle.reset(LifecycleSpyAgent.class);
        trace.reset();
        lifecycleSpyEvents.fire(new LifecycleSpyEvent("workflow-1"));

        assertEquals(1, lifecycle.constructed(LifecycleSpyAgent.class).size());
    }

    /**
     * {@code @PreDestroy} is invoked exactly once, on the workflow's instance, when the workflow
     * completes through its {@code @Outcome} and the {@code @WorkflowScoped} context is destroyed.
     *
     * <p>Specification section {@code lifecycle-callbacks}.
     */
    @Test
    public void workflowScopedAgentPreDestroyCalledAfterWorkflow() {
        lifecycle.reset(LifecycleSpyAgent.class);
        trace.reset();
        lifecycleSpyEvents.fire(new LifecycleSpyEvent("workflow-1"));

        assertEquals(Phase.OUTCOME, trace.phases().get(trace.phases().size() - 1));
        assertEquals(1, lifecycle.destroyed(LifecycleSpyAgent.class).size());
        assertEquals(lifecycle.constructed(LifecycleSpyAgent.class), lifecycle.destroyed(LifecycleSpyAgent.class));
    }

    /**
     * {@code @PreDestroy} is invoked exactly once, on the workflow's instance, when the workflow
     * fails. The {@code @Action} throws, the workflow never reaches its {@code @Outcome}, and the
     * {@code @WorkflowScoped} context is still torn down.
     *
     * <p>Specification section {@code lifecycle-callbacks}.
     */
    @Test
    public void workflowScopedAgentPreDestroyCalledAfterFailure() {
        lifecycle.reset(LifecycleSpyAgent.class);
        trace.reset();
        LifecycleSpyAgent.setFailInAction(true);
        try {
            lifecycleSpyEvents.fire(new LifecycleSpyEvent("workflow-1"));
        } catch (Exception expected) {
            // the failure propagates to the container
        } finally {
            LifecycleSpyAgent.setFailInAction(false);
        }

        assertFalse(trace.phases().contains(Phase.OUTCOME));
        assertEquals(1, lifecycle.destroyed(LifecycleSpyAgent.class).size());
        assertEquals(lifecycle.constructed(LifecycleSpyAgent.class), lifecycle.destroyed(LifecycleSpyAgent.class));
    }

    public record DecisionStopEvent(String payload) {}

    /** Its {@code @Decision} returns {@code false}, so the workflow ends before the action. */
    @Agent
    @WorkflowScoped
    public static class DecisionStopSpyAgent {

        @Inject
        ExecutionTraceRecorder trace;

        @Inject
        LifecycleRecorder lifecycle;

        private String instanceId;

        @PostConstruct
        void init() {
            instanceId = lifecycle.postConstruct(DecisionStopSpyAgent.class);
        }

        @PreDestroy
        void destroy() {
            lifecycle.preDestroy(DecisionStopSpyAgent.class, instanceId);
        }

        @Trigger
        public void onEvent(@Observes DecisionStopEvent event) {
            trace.record(Phase.TRIGGER, "onEvent", event, instanceId);
        }

        @Decision(order = 1)
        public boolean decide(DecisionStopEvent event) {
            trace.record(Phase.DECISION, "decide", event, instanceId);
            return false;
        }

        @Action(order = 2)
        public void process(DecisionStopEvent event) {
            trace.record(Phase.ACTION, "process", event, instanceId);
        }

        @Outcome
        public void finish(DecisionStopEvent event) {
            trace.record(Phase.OUTCOME, "finish", event, instanceId);
        }
    }

    /**
     * {@code @PreDestroy} is invoked exactly once, on the workflow's instance, when a
     * {@code @Decision} ends the workflow early. The context is destroyed although the
     * {@code @Action} and {@code @Outcome} never run.
     *
     * <p>Specification section {@code lifecycle-callbacks}.
     */
    @Test
    public void workflowScopedAgentPreDestroyCalledAfterDecisionStop() {
        lifecycle.reset(DecisionStopSpyAgent.class);
        trace.reset();
        decisionStopEvents.fire(new DecisionStopEvent("workflow-1"));

        assertEquals(List.of(Phase.TRIGGER, Phase.DECISION), trace.phases());
        assertEquals(1, lifecycle.destroyed(DecisionStopSpyAgent.class).size());
        assertEquals(
                lifecycle.constructed(DecisionStopSpyAgent.class), lifecycle.destroyed(DecisionStopSpyAgent.class));
    }

    public record SingletonCdiEvent(String payload) {}

    @Agent
    @ApplicationScoped
    public static class SingletonCdiAgent {

        @Inject
        ExecutionTraceRecorder trace;

        @Inject
        LifecycleRecorder lifecycle;

        private String instanceId;

        @PostConstruct
        void init() {
            instanceId = lifecycle.postConstruct(SingletonCdiAgent.class);
        }

        @Trigger
        public void onEvent(@Observes SingletonCdiEvent event) {
            trace.record(Phase.TRIGGER, "onEvent", event, instanceId);
        }

        @Action
        public void process(SingletonCdiEvent event) {
            trace.record(Phase.ACTION, "process", event, instanceId);
        }

        @Outcome
        public void finish(SingletonCdiEvent event) {
            trace.record(Phase.OUTCOME, "finish", event, instanceId);
        }
    }

    /**
     * {@code @PostConstruct} is invoked exactly once for an {@code @ApplicationScoped} agent
     * however many workflow events are fired.
     *
     * <p>Specification section {@code applicationscoped}.
     */
    @Test
    public void applicationScopedAgentPostConstructCalledOnce() {
        trace.reset();
        singletonEvents.fire(new SingletonCdiEvent("first"));
        singletonEvents.fire(new SingletonCdiEvent("second"));

        assertEquals(1, lifecycle.constructed(SingletonCdiAgent.class).size());
    }

    /**
     * An {@code @ApplicationScoped} agent reuses the same instance across sequential workflows.
     * Both {@code @Trigger} invocations record the same instance id.
     *
     * <p>Specification section {@code applicationscoped}.
     */
    @Test
    public void applicationScopedAgentSharesInstanceAcrossWorkflows() {
        trace.reset();
        singletonEvents.fire(new SingletonCdiEvent("first"));
        String id1 = (String) trace.entry(Phase.TRIGGER).args()[1];

        trace.reset();
        singletonEvents.fire(new SingletonCdiEvent("second"));
        String id2 = (String) trace.entry(Phase.TRIGGER).args()[1];

        assertEquals(id1, id2);
    }

    /**
     * An {@code @ApplicationScoped} agent uses the same instance across all phases of a
     * workflow. Trigger, action and outcome record the same instance id.
     *
     * <p>Specification section {@code applicationscoped}.
     */
    @Test
    public void applicationScopedAgentUsesSameInstanceAcrossAllPhases() {
        trace.reset();
        singletonEvents.fire(new SingletonCdiEvent("test"));

        assertEquals(List.of(Phase.TRIGGER, Phase.ACTION, Phase.OUTCOME), trace.phases());
        String triggerId = (String) trace.entry(Phase.TRIGGER).args()[1];
        String actionId = (String) trace.entry(Phase.ACTION).args()[1];
        String outcomeId = (String) trace.entry(Phase.OUTCOME).args()[1];
        assertEquals(triggerId, actionId);
        assertEquals(triggerId, outcomeId);
    }
}
