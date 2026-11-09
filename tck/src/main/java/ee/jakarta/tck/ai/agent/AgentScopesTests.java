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

import ee.jakarta.tck.ai.agent.agents.scopes.DecisionStopEvent;
import ee.jakarta.tck.ai.agent.agents.scopes.DecisionStopSpyAgent;
import ee.jakarta.tck.ai.agent.agents.scopes.LifecycleCallbackRecorder;
import ee.jakarta.tck.ai.agent.agents.scopes.LifecycleSpyAgent;
import ee.jakarta.tck.ai.agent.agents.scopes.LifecycleSpyEvent;
import ee.jakarta.tck.ai.agent.agents.scopes.ScopeDefaultAgent;
import ee.jakarta.tck.ai.agent.agents.scopes.ScopeDefaultEvent;
import ee.jakarta.tck.ai.agent.agents.scopes.SingletonCdiAgent;
import ee.jakarta.tck.ai.agent.agents.scopes.SingletonCdiEvent;
import ee.jakarta.tck.ai.agent.framework.ExecutionTraceRecorder;
import ee.jakarta.tck.ai.agent.framework.ExecutionTraceRecorder.Phase;
import jakarta.ai.agent.WorkflowScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
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
 * {@code @BeforeEach} does not run reliably between methods of an Arquillian test
 * class inside the container.</p>
 */
@ExtendWith(ArquillianExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AgentScopesTests {

    // bean-discovery-mode="all" is required so that ScopeDefaultAgent, which carries no CDI scope
    // annotation, is discovered and given the default @WorkflowScoped scope by the implementation.
    private static final String BEANS_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <beans xmlns="https://jakarta.ee/xml/ns/jakartaee"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee
                       https://jakarta.ee/xml/ns/jakartaee/beans_4_0.xsd"
                   version="4.0" bean-discovery-mode="all">
            </beans>
            """;

    @Deployment
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "agentscopes.war")
                .addClasses(
                        ScopeDefaultAgent.class,     ScopeDefaultEvent.class,
                        LifecycleSpyAgent.class,     LifecycleSpyEvent.class,
                        DecisionStopSpyAgent.class,  DecisionStopEvent.class,
                        LifecycleCallbackRecorder.class,
                        SingletonCdiAgent.class,     SingletonCdiEvent.class
                )
                .addAsWebInfResource(new StringAsset(BEANS_XML), "beans.xml");
    }

    @Inject Event<ScopeDefaultEvent>  scopeDefaultEvents;
    @Inject Event<LifecycleSpyEvent>  lifecycleSpyEvents;
    @Inject Event<DecisionStopEvent>  decisionStopEvents;
    @Inject Event<SingletonCdiEvent>  singletonEvents;
    @Inject ExecutionTraceRecorder    trace;
    @Inject LifecycleCallbackRecorder lifecycleRecorder;
    @Inject BeanManager               beanManager;

    /**
     * An agent with no explicit scope is {@code @WorkflowScoped}. One instance serves every phase
     * of a workflow, and each workflow gets a distinct instance.
     *
     * <p>Specification section {@code agent-lifecycle}.
     */
    @Test
    public void defaultScopeIsWorkflowScopedWithOneInstancePerWorkflow() {
        Set<Bean<?>> beans = beanManager.getBeans(ScopeDefaultAgent.class);
        assertEquals(1, beans.size());
        Bean<?> bean = beanManager.resolve(beans);
        assertNotNull(bean);
        assertEquals(WorkflowScoped.class, bean.getScope());

        lifecycleRecorder.reset();
        trace.reset();
        scopeDefaultEvents.fire(new ScopeDefaultEvent("workflow-1"));
        scopeDefaultEvents.fire(new ScopeDefaultEvent("workflow-2"));

        assertEquals(List.of(Phase.TRIGGER, Phase.ACTION, Phase.OUTCOME,
                Phase.TRIGGER, Phase.ACTION, Phase.OUTCOME), trace.phases());

        List<String> ids = lifecycleRecorder.getInstanceIds();
        assertEquals(2, ids.size());
        assertNotEquals(ids.get(1), ids.get(0));

        List<String> phaseInstanceIds = trace.entries().stream()
                .map(entry -> (String) entry.args()[1])
                .toList();
        assertEquals(List.of(ids.get(0), ids.get(0), ids.get(0),
                ids.get(1), ids.get(1), ids.get(1)), phaseInstanceIds);
    }

    /**
     * An explicitly {@code @WorkflowScoped} agent gets a distinct instance per workflow. Two
     * sequential events yield two different instance ids.
     *
     * <p>Specification section {@code cdi-integration}.
     */
    @Test
    public void workflowScopedAgentHasOneInstancePerWorkflow() {
        lifecycleRecorder.reset();
        trace.reset();
        lifecycleSpyEvents.fire(new LifecycleSpyEvent("workflow-1"));
        lifecycleSpyEvents.fire(new LifecycleSpyEvent("workflow-2"));
        List<String> ids = lifecycleRecorder.getInstanceIds();
        assertEquals(2, ids.size());
        assertNotEquals(ids.get(1), ids.get(0));
    }

    /**
     * {@code @PostConstruct} is invoked exactly once per {@code @WorkflowScoped} workflow
     * execution.
     *
     * <p>Specification section {@code cdi-integration}.
     */
    @Test
    public void workflowScopedAgentPostConstructCalledOnce() {
        lifecycleRecorder.reset();
        trace.reset();
        lifecycleSpyEvents.fire(new LifecycleSpyEvent("workflow-1"));
        assertEquals(1, lifecycleRecorder.getPostConstructCount());
    }

    /**
     * {@code @PreDestroy} is invoked exactly once when the workflow completes through its
     * {@code @Outcome} and the {@code @WorkflowScoped} context is destroyed.
     *
     * <p>Specification section {@code cdi-integration}.
     */
    @Test
    public void workflowScopedAgentPreDestroyCalledAfterWorkflow() {
        lifecycleRecorder.reset();
        trace.reset();
        lifecycleSpyEvents.fire(new LifecycleSpyEvent("workflow-1"));
        assertEquals(Phase.OUTCOME, trace.phases().get(trace.phases().size() - 1));
        assertEquals(1, lifecycleRecorder.getPreDestroyCount());
    }

    /**
     * {@code @PreDestroy} is invoked exactly once when the workflow fails. The {@code @Action}
     * throws, the workflow never reaches its {@code @Outcome}, and the {@code @WorkflowScoped}
     * context is still torn down.
     *
     * <p>Specification section {@code cdi-integration}.
     */
    @Test
    public void workflowScopedAgentPreDestroyCalledAfterFailure() {
        lifecycleRecorder.reset();
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
        assertEquals(1, lifecycleRecorder.getPreDestroyCount());
    }

    /**
     * {@code @PreDestroy} is invoked exactly once when a {@code @Decision} ends the workflow
     * early. The context is destroyed although the {@code @Action} and {@code @Outcome} never
     * run.
     *
     * <p>Specification section {@code cdi-integration}.
     */
    @Test
    public void workflowScopedAgentPreDestroyCalledAfterDecisionStop() {
        lifecycleRecorder.reset();
        trace.reset();
        decisionStopEvents.fire(new DecisionStopEvent("workflow-1"));
        assertEquals(List.of(Phase.TRIGGER, Phase.DECISION), trace.phases());
        assertEquals(1, lifecycleRecorder.getPreDestroyCount());
    }

    /**
     * {@code @PostConstruct} is invoked exactly once for an {@code @ApplicationScoped} agent
     * however many workflow events are fired.
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
     * An {@code @ApplicationScoped} agent reuses the same instance across sequential workflows.
     * Both {@code @Trigger} invocations record the same instance id.
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

        assertEquals(id1, id2);
    }

    /**
     * An {@code @ApplicationScoped} agent uses the same instance across all phases of a
     * workflow. Trigger, action and outcome record the same instance id.
     *
     * <p>Specification section {@code cdi-integration}.
     */
    @Test
    public void applicationScopedAgentUsesSameInstanceAcrossAllPhases() {
        trace.reset();
        singletonEvents.fire(new SingletonCdiEvent("test"));
        assertEquals(List.of(Phase.TRIGGER, Phase.ACTION, Phase.OUTCOME), trace.phases());
        int triggerId = (int) trace.entries().get(0).args()[1];
        int actionId  = (int) trace.entries().get(1).args()[1];
        int outcomeId = (int) trace.entries().get(2).args()[1];
        assertEquals(triggerId, actionId);
        assertEquals(triggerId, outcomeId);
    }
}
