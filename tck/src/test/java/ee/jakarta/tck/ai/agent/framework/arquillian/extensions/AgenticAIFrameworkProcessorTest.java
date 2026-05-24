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
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AgenticAIFrameworkProcessorTest {

    private final AgenticAIFrameworkProcessor processor = new AgenticAIFrameworkProcessor();

    @Test
    public void processorAddsLargeLanguageModelStub() {
        WebArchive archive = process(ShrinkWrap.create(WebArchive.class, "test.war"));

        assertThat(archive.contains(webClassPath(LargeLanguageModelStub.class)))
                .as("Processed WAR should contain LargeLanguageModelStub")
                .isTrue();
    }

    @Test
    public void processorAddsExecutionTraceRecorder() {
        WebArchive archive = process(ShrinkWrap.create(WebArchive.class, "test.war"));

        assertThat(archive.contains(webClassPath(ExecutionTraceRecorder.class)))
                .as("Processed WAR should contain ExecutionTraceRecorder")
                .isTrue();
    }

    @Test
    public void processorAddsAssertionAnnotation() {
        WebArchive archive = process(ShrinkWrap.create(WebArchive.class, "test.war"));

        assertThat(archive.contains(webClassPath(Assertion.class)))
                .as("Processed WAR should contain Assertion annotation")
                .isTrue();
    }

    @Test
    public void processorAddsAssertionExtension() {
        WebArchive archive = process(ShrinkWrap.create(WebArchive.class, "test.war"));

        assertThat(archive.contains(webClassPath(AssertionExtension.class)))
                .as("Processed WAR should contain AssertionExtension")
                .isTrue();
    }

    private WebArchive process(WebArchive archive) {
        processor.process(archive, null);
        return archive;
    }

    private static org.jboss.shrinkwrap.api.ArchivePath webClassPath(Class<?> clazz) {
        return ArchivePaths.create("/WEB-INF/classes/" + clazz.getName().replace('.', '/') + ".class");
    }
}
