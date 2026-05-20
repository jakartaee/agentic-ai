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

import ee.jakarta.tck.ai.agent.framework.stub.LargeLanguageModelStub;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder;
import ee.jakarta.tck.ai.agent.framework.junit.anno.Assertion;
import ee.jakarta.tck.ai.agent.framework.junit.extensions.AssertionExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AgenticAIFrameworkAppenderTest {

    private final AgenticAIFrameworkAppender appender = new AgenticAIFrameworkAppender();

    @Test
    public void archiveContainsLargeLanguageModelStub() {
        Archive<?> archive = appender.createAuxiliaryArchive();

        assertThat(archive.contains(classPath(LargeLanguageModelStub.class)))
                .as("Archive should contain LargeLanguageModelStub")
                .isTrue();
    }

    @Test
    public void archiveContainsExecutionTraceRecorder() {
        Archive<?> archive = appender.createAuxiliaryArchive();

        assertThat(archive.contains(classPath(ExecutionTraceRecorder.class)))
                .as("Archive should contain ExecutionTraceRecorder")
                .isTrue();
    }

    @Test
    public void archiveContainsAssertionAnnotation() {
        Archive<?> archive = appender.createAuxiliaryArchive();

        assertThat(archive.contains(classPath(Assertion.class)))
                .as("Archive should contain Assertion annotation")
                .isTrue();
    }

    @Test
    public void archiveContainsAssertionExtension() {
        Archive<?> archive = appender.createAuxiliaryArchive();

        assertThat(archive.contains(classPath(AssertionExtension.class)))
                .as("Archive should contain AssertionExtension")
                .isTrue();
    }

    private static org.jboss.shrinkwrap.api.ArchivePath classPath(Class<?> clazz) {
        return ArchivePaths.create("/" + clazz.getName().replace('.', '/') + ".class");
    }
}
