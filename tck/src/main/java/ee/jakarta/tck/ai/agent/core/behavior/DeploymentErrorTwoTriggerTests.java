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

import ee.jakarta.tck.ai.agent.core.behavior.agents.deploymenterror.DeploymentErrorEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.deploymenterror.SecondTriggerEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.deploymenterror.TwoTriggerAgent;
import ee.jakarta.tck.ai.agent.framework.junit.anno.Assertion;
import ee.jakarta.tck.ai.agent.framework.junit.anno.Deployed;
import ee.jakarta.tck.ai.agent.framework.junit.extensions.ImplementationPresentCondition;
import jakarta.enterprise.inject.spi.DeploymentException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.ShouldThrowException;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Deployment-error case G3c (<em>@Trigger cardinality</em>): an agent with more
 * than one {@code @Trigger} method must be rejected at deployment time.
 *
 * <p>See {@link DeploymentErrorTwoPhaseTests} for why this class is gated at the
 * class level with {@link EnabledIfSystemProperty} rather than a per-method
 * {@code @RequiresImplementation} condition.</p>
 */
@Deployed
@EnabledIfSystemProperty(named = ImplementationPresentCondition.IMPLEMENTATION_PRESENT_PROPERTY,
                         matches = "true")
public class DeploymentErrorTwoTriggerTests {

    @Deployment
    @ShouldThrowException(DeploymentException.class)
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "deployerror-twotrigger.war")
                .addClasses(TwoTriggerAgent.class, DeploymentErrorEvent.class, SecondTriggerEvent.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Assertion(id = "AGENTICAI-DEPLOY-ERR-003B",
               section = "agent-lifecycle",
               strategy = "An agent declaring more than one @Trigger method is a deployment error")
    public void multipleTriggersAreRejected() {
        // Verified by @ShouldThrowException on the deployment.
    }
}