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
import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * Arquillian archive processor that injects TCK framework classes directly
 * into every {@link WebArchive} before it is deployed to the container.
 *
 * <p>Unlike an {@code AuxiliaryArchiveAppender}, this processor adds classes
 * to the archive itself, which is required for Weld Embedded to discover them
 * as CDI beans during bean validation.</p>
 */
public class AgenticAIFrameworkProcessor implements ApplicationArchiveProcessor {

    @Override
    public void process(Archive<?> applicationArchive, TestClass testClass) {
        if (!(applicationArchive instanceof WebArchive webArchive)) {
            return;
        }
        webArchive.addPackages(false,
                Assertion.class.getPackage(),
                AssertionExtension.class.getPackage(),
                LargeLanguageModelStub.class.getPackage(),
                ExecutionTraceRecorder.class.getPackage()
        );
    }
}
