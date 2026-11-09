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

import ee.jakarta.tck.ai.agent.agents.deploymenterror.TwoPhaseMethodAgent;
import ee.jakarta.tck.ai.agent.agents.deploymenterror.TwoPhaseMethodEvent;
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
 * An agent whose method declares two phase annotations must be rejected at
 * deployment time (<em>One Phase per Method</em>).
 *
 * <p>This error is detected by a compatible implementation's CDI extension.
 * The check is the deployment itself.</p>
 *
 * <p>The verification is the deployment itself: {@link ShouldThrowException}
 * declares that building the container for this archive must fail with a
 * {@link DeploymentException}; the test body is therefore empty.</p>
 */
@ExtendWith(ArquillianExtension.class)
public class DeploymentErrorTwoPhaseTests {

    @Deployment
    @ShouldThrowException(DeploymentException.class)
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "deployerror-twophase.war")
                .addClasses(TwoPhaseMethodAgent.class, TwoPhaseMethodEvent.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    /**
     * Combining two phase annotations on one method is a deployment error rejected at deploy time.
     *
     * <p>Specification section {@code agent-lifecycle}.
     */
    @Test
    public void twoPhaseMethodIsRejected() {
        // Verified by @ShouldThrowException on the deployment.
    }
}