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

import ee.jakarta.tck.ai.agent.behavior.agents.deploymenterror.DeploymentErrorEvent;
import ee.jakarta.tck.ai.agent.behavior.agents.deploymenterror.MultiEventTriggerAgent;
import ee.jakarta.tck.ai.agent.behavior.agents.deploymenterror.SecondTriggerEvent;
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
 * Deployment-error case G3c (<em>exactly one event parameter</em>): a
 * {@code @Trigger} method declaring more than one event parameter must be rejected
 * at deployment time.
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

    /**
     * A {@code @Trigger} method declaring more than one event parameter is a deployment error.
     *
     * <p>Specification section {@code agent-lifecycle}.
     */
    @Test
    public void multipleEventParametersAreRejected() {
        // Verified by @ShouldThrowException on the deployment.
    }
}