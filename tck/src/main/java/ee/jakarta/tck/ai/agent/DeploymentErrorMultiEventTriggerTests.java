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
import jakarta.enterprise.context.ApplicationScoped;
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
 * A {@code @Trigger} method declaring more than one event parameter must be
 * rejected at deployment time (<em>exactly one event parameter</em>).
 */
@ExtendWith(ArquillianExtension.class)
public class DeploymentErrorMultiEventTriggerTests {

    @Deployment
    @ShouldThrowException(DeploymentException.class)
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "deployerror-multievent.war")
                .addClasses(MultiEventTriggerAgent.class, DeploymentErrorEvent.class, SecondTriggerEvent.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    public record DeploymentErrorEvent(String payload) {}

    public record SecondTriggerEvent(String payload) {}

    /**
     * Neither event parameter carries {@code @Observes}, so plain CDI accepts the method and
     * only the rule that a trigger has exactly one event parameter can reject this agent.
     */
    @Agent
    @ApplicationScoped
    public static class MultiEventTriggerAgent {

        @Trigger
        public void onEvent(DeploymentErrorEvent first, SecondTriggerEvent second) {}
    }

    /**
     * A {@code @Trigger} method declaring more than one event parameter is a deployment error.
     *
     * <p>Specification section {@code trigger-parameters}.
     */
    @Test
    public void multipleEventParametersAreRejected() {
        // Verified by @ShouldThrowException on the deployment.
    }
}
