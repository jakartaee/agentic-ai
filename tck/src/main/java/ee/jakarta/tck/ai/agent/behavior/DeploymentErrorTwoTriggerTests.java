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
import ee.jakarta.tck.ai.agent.behavior.agents.deploymenterror.SecondTriggerEvent;
import ee.jakarta.tck.ai.agent.behavior.agents.deploymenterror.TwoTriggerAgent;
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
 * An agent with more than one {@code @Trigger} method must be rejected at
 * deployment time (<em>@Trigger cardinality</em>).
 */
@ExtendWith(ArquillianExtension.class)
public class DeploymentErrorTwoTriggerTests {

    @Deployment
    @ShouldThrowException(DeploymentException.class)
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "deployerror-twotrigger.war")
                .addClasses(TwoTriggerAgent.class, DeploymentErrorEvent.class, SecondTriggerEvent.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    /**
     * An agent declaring more than one {@code @Trigger} method is a deployment error.
     *
     * <p>Specification section {@code agent-lifecycle}.
     */
    @Test
    public void multipleTriggersAreRejected() {
        // Verified by @ShouldThrowException on the deployment.
    }
}