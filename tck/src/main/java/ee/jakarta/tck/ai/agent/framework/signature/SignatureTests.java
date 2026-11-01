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
package ee.jakarta.tck.ai.agent.framework.signature;

import com.sun.tdk.signaturetest.SignatureTest;
import ee.jakarta.tck.ai.agent.framework.junit.anno.Assertion;
import ee.jakarta.tck.ai.agent.framework.junit.anno.Standalone;
import jakarta.ai.agent.Agent;
import jakarta.enterprise.context.NormalScope;
import jakarta.enterprise.util.AnnotationLiteral;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the public API surface of {@code jakarta.ai.agent} against the
 * signature baseline shipped in the TCK, using the Jakarta TCK signature
 * test engine.
 *
 * <p>The check is strict in both directions: a removed, changed <em>or
 * added</em> public member fails it. JDK types are resolved from the running
 * JDK's {@code lib/ct.sym} view of Java SE {@value #JAVA_SE_RELEASE}, the
 * baseline the signature file was recorded against, so the result does not
 * depend on which newer JDK runs the TCK. The test therefore needs a JDK,
 * not a JRE.</p>
 *
 * <p>The API under test is whichever {@code jakarta.ai.agent} the test
 * class path resolves, so an implementation that bundles its own copy of the
 * API is verified against that copy.</p>
 */
@Standalone
public class SignatureTests {

    static final String PACKAGE = "jakarta.ai.agent";
    static final String SIGNATURE_RESOURCE =
            "ee/jakarta/tck/ai/agent/framework/signature/jakarta.ai.agent.sig_1.0";
    static final int JAVA_SE_RELEASE = 17;

    @Assertion(id = "AGENTICAI-SIG-001",
               strategy = "Run the Jakarta TCK signature test engine in strict static mode "
                       + "against the recorded jakarta.ai.agent signature file")
    public void apiMatchesRecordedSignature() throws IOException, URISyntaxException {
        Path signatureFile = Files.createTempFile("jakarta.ai.agent", ".sig");
        try {
            try (InputStream in = SignatureTests.class.getClassLoader().getResourceAsStream(SIGNATURE_RESOURCE)) {
                assertNotNull(in, "Signature baseline " + SIGNATURE_RESOURCE + " is missing from the TCK");
                Files.copy(in, signatureFile, StandardCopyOption.REPLACE_EXISTING);
            }
            Path apiLocation = locationOf(Agent.class);
            // The CDI types the API extends and is annotated with must be resolvable too.
            String classpath = Stream.of(apiLocation, locationOf(NormalScope.class), locationOf(AnnotationLiteral.class))
                    .distinct()
                    .map(Path::toString)
                    .collect(Collectors.joining(File.pathSeparator));

            String[] args = {
                    "-Static",
                    "-FileName", signatureFile.toString(),
                    "-PackageWithoutSubpackages", PACKAGE,
                    "-BootCP", Integer.toString(JAVA_SE_RELEASE),
                    "-Classpath", classpath
            };
            StringWriter report = new StringWriter();
            SignatureTest test = new SignatureTest();
            test.run(args, new PrintWriter(report, true), null);

            assertTrue(test.isPassed(), () -> PACKAGE + " at " + apiLocation
                    + " differs from the recorded signature baseline:" + System.lineSeparator() + report);
        } finally {
            Files.deleteIfExists(signatureFile);
        }
    }

    private static Path locationOf(Class<?> type) throws URISyntaxException {
        return Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI());
    }
}
