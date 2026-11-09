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

import ee.jakarta.tck.ai.agent.core.behavior.agents.cdi.DecisionStopEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.cdi.DecisionStopSpyAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.cdi.LifecycleCallbackRecorder;
import ee.jakarta.tck.ai.agent.core.behavior.agents.cdi.LifecycleSpyAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.cdi.LifecycleSpyEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.cdi.ScopeDefaultAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.cdi.ScopeDefaultEvent;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder.Phase;
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

@ExtendWith(ArquillianExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class WorkflowScopeLifecycleTests {

    // bean-discovery-mode="all" is required so that ScopeDefaultAgent (which has no
    // explicit CDI scope annotation) is discovered and processed by the implementation,
    // which adds the default @WorkflowScoped scope to it.
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
        return ShrinkWrap.create(WebArchive.class, "workflowscopelifecycle.war")
                .addClasses(
                        LifecycleSpyAgent.class,        LifecycleSpyEvent.class,
                        ScopeDefaultAgent.class,        ScopeDefaultEvent.class,
                        DecisionStopSpyAgent.class,     DecisionStopEvent.class,
                        LifecycleCallbackRecorder.class
                )
                .addAsWebInfResource(new StringAsset(BEANS_XML), "beans.xml");
    }

    @Inject Event<LifecycleSpyEvent>  lifecycleSpyEvents;
    @Inject Event<ScopeDefaultEvent>  scopeDefaultEvents;
    @Inject Event<DecisionStopEvent>  decisionStopEvents;
    @Inject ExecutionTraceRecorder    trace;
    @Inject LifecycleCallbackRecorder lifecycleRecorder;
    @Inject BeanManager               beanManager;
    /**
     * An {@code @Agent} with no explicit scope is registered as {@code @WorkflowScoped}, uses one
     * instance across every phase, and creates a distinct instance for each workflow. Sequential
     * events are used as a v1 proxy for concurrent workflows.
     *
     * <p>Specification section {@code agent-lifecycle}.
     */
    @Test
    public void defaultScopeProducesDistinctInstancePerWorkflow() {
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
     * An {@code @Agent} with no explicit name runs its full workflow. The derived name itself is
     * not asserted: the specification does not yet say how an agent name is exposed.
     *
     * <p>Specification section {@code agent-lifecycle}.
     */
    @Test
    public void scopeDefaultAgentRunsFullWorkflow() {
        trace.reset();
        scopeDefaultEvents.fire(new ScopeDefaultEvent("naming"));
        assertEquals(List.of(Phase.TRIGGER, Phase.ACTION, Phase.OUTCOME), trace.phases());
    }
    /**
     * An {@code @Agent} with an explicit name, here {@code @Agent}(name = "lifecycleSpy"), runs
     * its full workflow. The name is not asserted as a CDI bean name.
     *
     * <p>Specification section {@code agent-lifecycle}.
     */
    @Test
    public void lifecycleSpyAgentRunsFullWorkflow() {

        trace.reset();
        lifecycleSpyEvents.fire(new LifecycleSpyEvent("naming"));
        assertEquals(List.of(Phase.TRIGGER, Phase.ACTION, Phase.OUTCOME), trace.phases());
    }
    /**
     * {@code @PostConstruct} is invoked exactly once per {@code @WorkflowScoped} workflow
     * execution — LifecycleCallbackRecorder counts one {@code @PostConstruct} call after a single
     * workflow.
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
     * {@code @PreDestroy} is invoked exactly once after the {@code @WorkflowScoped} context is
     * destroyed (workflow completes through {@code @Outcome}) — LifecycleCallbackRecorder counts
     * one call.
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
     * {@code @PreDestroy} is invoked exactly once even when the workflow FAILS (the issue requires
     * cleanup 'after the workflow completes or fails'). LifecycleSpyAgent.setFailInAction(true)
     * makes {@code @Action} throw; the compatible implementation must still tear down the {@code
     * @WorkflowScoped} context and the workflow must not reach {@code @Outcome}.
     *
     * <p>Specification section {@code cdi-integration}.
     */
    @Test
    public void workflowScopedAgentPreDestroyCalledAfterFailure() {
        lifecycleRecorder.reset();
        trace.reset();
        LifecycleSpyAgent.setFailInAction(true);
        try {
            // @Action throws and the failure propagates to the container; catch and ignore
            lifecycleSpyEvents.fire(new LifecycleSpyEvent("workflow-1"));
        } catch (Exception ignored) {
        } finally {
            LifecycleSpyAgent.setFailInAction(false);
        }
        assertFalse(trace.phases().contains(Phase.OUTCOME));
        assertEquals(1, lifecycleRecorder.getPreDestroyCount());
    }
    /**
     * {@code @PreDestroy} is invoked exactly once when the workflow terminates at a {@code
     * @Decision} returning false: the {@code @WorkflowScoped} context is destroyed although {@code
     * @Action} and {@code @Outcome} never run.
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
     * {@code @WorkflowScoped} creates a distinct bean instance per workflow execution — two
     * sequential events yield two different instanceIds (v1 proxy for concurrent workflows).
     *
     * <p>Specification section {@code cdi-integration}.
     */
    @Test
    public void workflowScopedAgentHasUniqueInstancePerWorkflow() {
        lifecycleRecorder.reset();
        trace.reset();
        lifecycleSpyEvents.fire(new LifecycleSpyEvent("workflow-1"));
        lifecycleSpyEvents.fire(new LifecycleSpyEvent("workflow-2"));
        List<String> ids = lifecycleRecorder.getInstanceIds();
        assertEquals(2, ids.size());
        assertNotEquals(ids.get(1), ids.get(0));
    }
}
