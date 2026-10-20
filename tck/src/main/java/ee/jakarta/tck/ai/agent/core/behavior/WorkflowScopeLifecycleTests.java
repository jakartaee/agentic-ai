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

import ee.jakarta.tck.ai.agent.core.behavior.agents.cdi.LifecycleCallbackRecorder;
import ee.jakarta.tck.ai.agent.core.behavior.agents.cdi.LifecycleSpyAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.cdi.LifecycleSpyEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.cdi.ScopeDefaultAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.cdi.ScopeDefaultEvent;
import ee.jakarta.tck.ai.agent.framework.junit.anno.Assertion;
import ee.jakarta.tck.ai.agent.framework.junit.anno.Deployed;
import ee.jakarta.tck.ai.agent.framework.junit.anno.RequiresImplementation;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder.Phase;
import jakarta.ai.agent.WorkflowScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.TestInstance;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Deployed
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
                        LifecycleCallbackRecorder.class
                )
                .addAsWebInfResource(new StringAsset(BEANS_XML), "beans.xml");
    }

    @Inject Event<LifecycleSpyEvent>  lifecycleSpyEvents;
    @Inject Event<ScopeDefaultEvent>  scopeDefaultEvents;
    @Inject ExecutionTraceRecorder    trace;
    @Inject LifecycleCallbackRecorder lifecycleRecorder;
    @Inject BeanManager               beanManager;

    @RequiresImplementation
    @Assertion(id = "AGENTICAI-CDI-BHV-001",
               section = "agent-lifecycle",
               strategy = "An @Agent with no explicit scope is registered as @WorkflowScoped, uses one "
                        + "instance across every phase, and creates a distinct instance for each workflow. "
                        + "Sequential events are used as a v1 proxy for concurrent workflows.")
    public void defaultScopeProducesDistinctInstancePerWorkflow() {
        Set<Bean<?>> beans = beanManager.getBeans(ScopeDefaultAgent.class);
        assertThat(beans).hasSize(1);
        Bean<?> bean = beanManager.resolve(beans);
        assertThat(bean).isNotNull();
        assertThat(bean.getScope()).isEqualTo(WorkflowScoped.class);

        lifecycleRecorder.reset();
        trace.reset();
        scopeDefaultEvents.fire(new ScopeDefaultEvent("workflow-1"));
        scopeDefaultEvents.fire(new ScopeDefaultEvent("workflow-2"));

        assertThat(trace.phases()).containsExactly(
                Phase.TRIGGER, Phase.ACTION, Phase.OUTCOME,
                Phase.TRIGGER, Phase.ACTION, Phase.OUTCOME);

        List<String> ids = lifecycleRecorder.getInstanceIds();
        assertThat(ids).hasSize(2);
        assertThat(ids.get(0)).isNotEqualTo(ids.get(1));

        List<String> phaseInstanceIds = trace.entries().stream()
                .map(entry -> (String) entry.args()[1])
                .toList();
        assertThat(phaseInstanceIds).containsExactly(
                ids.get(0), ids.get(0), ids.get(0),
                ids.get(1), ids.get(1), ids.get(1));
    }

    @RequiresImplementation
    @Assertion(id = "AGENTICAI-CDI-BHV-002",
               section = "agent-lifecycle",
               strategy = "An @Agent with no explicit name runs its full workflow. A trigger-only plain-CDI "
                        + "run cannot satisfy this assertion because the implementation must also dispatch "
                        + "@Action and @Outcome. The derived name itself is not asserted: the specification does "
                        + "not yet say how an agent name is exposed.")
    public void scopeDefaultAgentRunsFullWorkflow() {
        trace.reset();
        scopeDefaultEvents.fire(new ScopeDefaultEvent("naming"));
        assertThat(trace.phases()).containsExactly(Phase.TRIGGER, Phase.ACTION, Phase.OUTCOME);
    }

    @RequiresImplementation
    @Assertion(id = "AGENTICAI-CDI-BHV-002",
               section = "agent-lifecycle",
               strategy = "An @Agent with an explicit name, here @Agent(name = \"lifecycleSpy\"), runs its full "
                        + "workflow. A trigger-only plain-CDI run cannot satisfy this assertion because the "
                        + "implementation must also dispatch @Action and @Outcome. The name is not asserted as a "
                        + "CDI bean name.")
    public void lifecycleSpyAgentRunsFullWorkflow() {
        assertThat(LifecycleSpyAgent.class.getAnnotation(jakarta.ai.agent.Agent.class).name())
                .isEqualTo("lifecycleSpy");

        trace.reset();
        lifecycleSpyEvents.fire(new LifecycleSpyEvent("naming"));
        assertThat(trace.phases()).containsExactly(Phase.TRIGGER, Phase.ACTION, Phase.OUTCOME);
    }

    @RequiresImplementation
    @Assertion(id = "AGENTICAI-CDI-BHV-003",
               section = "cdi-integration",
               strategy = "@PostConstruct is invoked exactly once per @WorkflowScoped workflow execution — "
                        + "LifecycleCallbackRecorder counts one @PostConstruct call after a single workflow")
    public void workflowScopedAgentPostConstructCalledOnce() {
        lifecycleRecorder.reset();
        trace.reset();
        lifecycleSpyEvents.fire(new LifecycleSpyEvent("workflow-1"));
        assertThat(lifecycleRecorder.getPostConstructCount()).isEqualTo(1);
    }

    @RequiresImplementation
    @Assertion(id = "AGENTICAI-CDI-BHV-003",
               section = "cdi-integration",
               strategy = "@PreDestroy is invoked exactly once after the @WorkflowScoped context is destroyed "
                        + "(workflow completes through @Outcome) — LifecycleCallbackRecorder counts one call")
    public void workflowScopedAgentPreDestroyCalledAfterWorkflow() {
        lifecycleRecorder.reset();
        trace.reset();
        lifecycleSpyEvents.fire(new LifecycleSpyEvent("workflow-1"));
        assertThat(trace.phases()).endsWith(Phase.OUTCOME);
        assertThat(lifecycleRecorder.getPreDestroyCount()).isEqualTo(1);
    }

    @RequiresImplementation
    @Assertion(id = "AGENTICAI-CDI-BHV-003",
               section = "cdi-integration",
               strategy = "@PreDestroy is invoked exactly once even when the workflow FAILS (the issue requires "
                        + "cleanup 'after the workflow completes or fails'). LifecycleSpyAgent.setFailInAction(true) "
                        + "makes @Action throw; the compatible implementation must still tear down the @WorkflowScoped context and the "
                        + "workflow must not reach @Outcome")
    public void workflowScopedAgentPreDestroyCalledAfterFailure() {
        lifecycleRecorder.reset();
        trace.reset();
        LifecycleSpyAgent.setFailInAction(true);
        try {
            // @Action throws; CDI wraps in ObserverException — catch and ignore
            lifecycleSpyEvents.fire(new LifecycleSpyEvent("workflow-1"));
        } catch (Exception ignored) {
            // expected: the action failure propagates wrapped in ObserverException
        } finally {
            LifecycleSpyAgent.setFailInAction(false);
        }
        assertThat(trace.phases()).doesNotContain(Phase.OUTCOME);
        assertThat(lifecycleRecorder.getPreDestroyCount()).isEqualTo(1);
    }

    @RequiresImplementation
    @Assertion(id = "AGENTICAI-CDI-BHV-004",
               section = "cdi-integration",
               strategy = "@WorkflowScoped creates a distinct bean instance per workflow execution — two "
                        + "sequential events yield two different instanceIds (v1 proxy for concurrent workflows)")
    public void workflowScopedAgentHasUniqueInstancePerWorkflow() {
        lifecycleRecorder.reset();
        trace.reset();
        lifecycleSpyEvents.fire(new LifecycleSpyEvent("workflow-1"));
        lifecycleSpyEvents.fire(new LifecycleSpyEvent("workflow-2"));
        List<String> ids = lifecycleRecorder.getInstanceIds();
        assertThat(ids).hasSize(2);
        assertThat(ids.get(0)).isNotEqualTo(ids.get(1));
    }
}
