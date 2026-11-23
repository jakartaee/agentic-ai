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

import jakarta.ai.agent.Agent;
import jakarta.ai.agent.Trigger;
import jakarta.ai.agent.WorkflowScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.DeploymentException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.ShouldThrowException;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * A {@code @WorkflowScoped} agent with a general observer method must be
 * rejected at deployment time (<em>Event Observation Rules</em>).
 */
@ExtendWith(ArquillianExtension.class)
public class DeploymentErrorWorkflowScopedObserverTests {

    @Deployment
    @ShouldThrowException(DeploymentException.class)
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "deployerror-workflowscopedobserver.war")
                .addClasses(
                        WorkflowScopedGeneralObserverAgent.class,
                        DeploymentErrorEvent.class,
                        GeneralObserverEvent.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    public record DeploymentErrorEvent(String payload) {}

    public record GeneralObserverEvent(String payload) {}

    @Agent
    @WorkflowScoped
    public static class WorkflowScopedGeneralObserverAgent {

        @Trigger
        public void onEvent(@Observes DeploymentErrorEvent event) {}

        public void onGeneralEvent(@Observes GeneralObserverEvent event) {}
    }

    /**
     * A {@code @WorkflowScoped} agent that declares an {@code @Observes} parameter on a
     * method other than its {@code @Trigger} is a deployment error.
     *
     * <p>Specification section {@code cdi-events}.
     */
    @Test
    public void generalObserverOnWorkflowScopedAgentIsRejected() {
        // Verified by @ShouldThrowException on the deployment.
    }
}
