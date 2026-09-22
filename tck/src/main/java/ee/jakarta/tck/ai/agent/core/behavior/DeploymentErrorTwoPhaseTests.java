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

import ee.jakarta.tck.ai.agent.core.behavior.agents.deploymenterror.TwoPhaseMethodAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.deploymenterror.TwoPhaseMethodEvent;
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
 * Deployment-error case G3a (<em>One Phase per Method</em>): an agent whose
 * method declares two phase annotations must be rejected at deployment time.
 *
 * <p>This error is detected by a compatible implementation's CDI extension.
 * Plain CDI ignores the extra annotation and deploys successfully, so without an
 * implementation the deployment does not fail. Unlike the behavioral
 * {@code @RequiresImplementation} tests, the guard here cannot be a per-method
 * condition: the {@link ShouldThrowException} check runs at deployment time, in
 * the client JVM, before the in-container implementation probe. The whole class
 * is therefore gated at the class level with {@link EnabledIfSystemProperty},
 * which reads {@value ImplementationPresentCondition#IMPLEMENTATION_PRESENT_PROPERTY}
 * directly in the client JVM so the deployment is skipped entirely when no
 * compatible implementation is present.</p>
 *
 * <p>The verification is the deployment itself: {@link ShouldThrowException}
 * declares that building the container for this archive must fail with a
 * {@link DeploymentException}; the test body is therefore empty.</p>
 */
@Deployed
@EnabledIfSystemProperty(named = ImplementationPresentCondition.IMPLEMENTATION_PRESENT_PROPERTY,
                         matches = "true")
public class DeploymentErrorTwoPhaseTests {

    @Deployment
    @ShouldThrowException(DeploymentException.class)
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "deployerror-twophase.war")
                .addClasses(TwoPhaseMethodAgent.class, TwoPhaseMethodEvent.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Assertion(id = "AGENTICAI-DEPLOY-ERR-001",
               section = "agent-lifecycle",
               strategy = "Combining two phase annotations on one method is a deployment error rejected at deploy time")
    public void twoPhaseMethodIsRejected() {
        // Verified by @ShouldThrowException on the deployment.
    }
}