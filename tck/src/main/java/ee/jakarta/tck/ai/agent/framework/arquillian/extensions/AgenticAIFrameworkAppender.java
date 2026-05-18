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
package ee.jakarta.tck.ai.agent.framework.arquillian.extensions;

import ee.jakarta.tck.ai.agent.framework.junit.anno.Assertion;
import ee.jakarta.tck.ai.agent.framework.junit.extensions.AssertionExtension;
import ee.jakarta.tck.ai.agent.framework.stub.LargeLanguageModelStub;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder;
import org.jboss.arquillian.container.test.spi.client.deployment.CachedAuxilliaryArchiveAppender;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

/**
 * Arquillian auxiliary archive appender that automatically bundles the TCK
 * framework classes into every deployment archive.
 *
 * <p>This eliminates the need for individual {@code @Deployment} methods to
 * manually add {@link LargeLanguageModelStub} and {@link ExecutionTraceRecorder}
 * to each archive.</p>
 */
public class AgenticAIFrameworkAppender extends CachedAuxilliaryArchiveAppender {

    @Override
    protected Archive<?> buildArchive() {
        return ShrinkWrap.create(JavaArchive.class, "jakarta-agentic-ai-framework.jar")
                .addPackages(false,
                        Assertion.class.getPackage(),
                        AssertionExtension.class.getPackage(),
                        LargeLanguageModelStub.class.getPackage(),
                        ExecutionTraceRecorder.class.getPackage()
                );
    }
}
