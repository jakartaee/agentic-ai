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
import jakarta.enterprise.context.Dependent;
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
 * An agent whose scope is neither {@code @WorkflowScoped} nor
 * {@code @ApplicationScoped} must be rejected at deployment time
 * (<em>Agent Scopes</em>).
 */
@ExtendWith(ArquillianExtension.class)
public class DeploymentErrorAgentScopeTests {

    @Deployment
    @ShouldThrowException(DeploymentException.class)
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "deployerror-agentscope.war")
                .addClasses(DependentScopedAgent.class, DeploymentErrorEvent.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    public record DeploymentErrorEvent(String payload) {}

    @Agent
    @Dependent
    public static class DependentScopedAgent {

        @Trigger
        public void onEvent(@Observes DeploymentErrorEvent event) {
        }
    }

    /**
     * A {@code @Dependent} agent is a deployment error.
     *
     * <p>Specification section {@code agent-scopes}.
     */
    @Test
    public void dependentAgentIsRejected() {
        // Verified by @ShouldThrowException on the deployment.
    }
}
