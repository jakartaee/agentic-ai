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
import jakarta.ai.agent.Agent;
import jakarta.enterprise.context.NormalScope;
import jakarta.enterprise.util.AnnotationLiteral;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the public API surface of {@code jakarta.ai.agent} against the
 * signature file shipped in the TCK, using the Jakarta TCK signature
 * test engine.
 *
 * <p>Like every other test, this one deploys to the implementation and runs
 * inside it, so the API it verifies is the {@code jakarta.ai.agent} the
 * implementation provides to applications: the API jar the runtime ships, or
 * the implementation jar if it bundles the API. The deployment carries the
 * signature file and the engine itself, both taken from the test client's
 * class path.</p>
 *
 * <p>The check is strict in both directions. A removed, changed <em>or
 * added</em> public member fails it. JDK types are resolved at the Java SE
 * {@value #JAVA_SE_RELEASE} level, the baseline the signature file was
 * recorded against, so the result does not depend on which newer JDK runs
 * the implementation.</p>
 */
@ExtendWith(ArquillianExtension.class)
public class SignatureTests {

    static final String PACKAGE = "jakarta.ai.agent";
    static final String SIGNATURE_RESOURCE =
            "ee/jakarta/tck/ai/agent/framework/signature/jakarta.ai.agent.sig_1.0";
    static final int JAVA_SE_RELEASE = 17;

    /** The engine's own classes and the Java SE API data it resolves JDK types from. */
    private static final String[] ENGINE_ENTRIES = { "com/sun/tdk/", "META-INF/sigtest" };

    @Deployment
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "signature.war")
                .addAsLibrary(signatureEngine())
                .addAsResource(SIGNATURE_RESOURCE);
    }

    /**
     * Run the Jakarta TCK signature test engine in strict static mode against the recorded
     * jakarta.ai.agent signature file.
     *
     * <p>Specification section {@code programming-model}.
     */
    @Test
    public void apiMatchesRecordedSignature() throws IOException, URISyntaxException {
        Path signatureFile = Files.createTempFile("jakarta.ai.agent", ".sig");
        try {
            try (InputStream in = SignatureTests.class.getClassLoader().getResourceAsStream(SIGNATURE_RESOURCE)) {
                assertNotNull(in, "Signature file " + SIGNATURE_RESOURCE + " is missing from the deployment");
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
                    + " differs from the recorded signature file:" + System.lineSeparator() + report);
        } finally {
            Files.deleteIfExists(signatureFile);
        }
    }

    /**
     * Repackages the signature test engine from the jar it was loaded from on
     * the test client: its own classes and the Java SE API data they read, not
     * the Maven plugin layer the same jar carries.
     */
    private static JavaArchive signatureEngine() {
        JavaArchive engine = ShrinkWrap.create(JavaArchive.class, "sigtest.jar");
        try (JarFile jar = new JarFile(locationOf(SignatureTest.class).toFile())) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory() || Stream.of(ENGINE_ENTRIES).noneMatch(entry.getName()::startsWith)) {
                    continue;
                }
                try (InputStream in = jar.getInputStream(entry)) {
                    engine.add(new ByteArrayAsset(in.readAllBytes()), entry.getName());
                }
            }
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException("Cannot repackage the signature test engine for deployment", e);
        }
        return engine;
    }

    /**
     * The jar or directory a class was loaded from. A server may report the
     * code source as a {@code jar:} URL; the file behind it is what the engine
     * reads.
     */
    private static Path locationOf(Class<?> type) throws URISyntaxException {
        URI location = type.getProtectionDomain().getCodeSource().getLocation().toURI();
        if ("jar".equals(location.getScheme())) {
            String inner = location.getSchemeSpecificPart();
            int separator = inner.indexOf("!/");
            location = URI.create(separator == -1 ? inner : inner.substring(0, separator));
        }
        try {
            return Path.of(location);
        } catch (RuntimeException e) {
            throw new IllegalStateException(type.getName() + " was not loaded from a file system location: " + location, e);
        }
    }
}
