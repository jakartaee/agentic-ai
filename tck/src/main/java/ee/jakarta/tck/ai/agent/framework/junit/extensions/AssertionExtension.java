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
package ee.jakarta.tck.ai.agent.framework.junit.extensions;

import ee.jakarta.tck.ai.agent.framework.junit.anno.Assertion;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * JUnit 5 extension that logs information about specification assertions.
 * It tracks the execution of methods annotated with {@link Assertion}.
 */
public class AssertionExtension implements TestWatcher, BeforeTestExecutionCallback, AfterTestExecutionCallback {

    private static final Logger LOGGER = Logger.getLogger(AssertionExtension.class.getName());
    private static final String NL = System.lineSeparator();

    @Override
    public void beforeTestExecution(ExtensionContext context) {
        context.getTestMethod().ifPresent(method -> {
            Assertion assertion = method.getAnnotation(Assertion.class);
            if (assertion != null) {
                LOGGER.info(() -> "Executing @Assertion #" + assertion.id() + " - " + method.getName());
            }
        });
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
        context.getTestMethod().ifPresent(method -> {
            Assertion assertion = method.getAnnotation(Assertion.class);
            if (assertion != null) {
                LOGGER.info(() -> "<<< End @Assertion #" + assertion.id() + " - " + method.getName());
            }
        });
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        logAssertionFailure(context, cause, "failed");
    }

    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        logAssertionFailure(context, cause, "aborted");
    }

    @Override
    public void testDisabled(ExtensionContext context, Optional<String> reason) {
        context.getTestMethod().ifPresent(method -> {
            Assertion assertion = method.getAnnotation(Assertion.class);
            if (assertion != null) {
                LOGGER.warning(() -> method.getName() + " disabled" + NL
                        + " @Assertion.id: #" + assertion.id() + NL
                        + " @Assertion.section: " + assertion.section() + NL
                        + " @Assertion.strategy: " + assertion.strategy() + NL
                        + " Reason: " + reason.orElse("N/A"));
            }
        });
    }

    private void logAssertionFailure(ExtensionContext context, Throwable cause, String status) {
        context.getTestMethod().ifPresent(method -> {
            Assertion assertion = method.getAnnotation(Assertion.class);
            if (assertion != null) {
                LOGGER.warning(() -> method.getName() + " " + status + NL
                        + " @Assertion.id: #" + assertion.id() + NL
                        + " @Assertion.section: " + assertion.section() + NL
                        + " @Assertion.strategy: " + assertion.strategy() + NL
                        + " Throwable.cause: " + (cause != null ? cause.getLocalizedMessage() : "N/A"));
            }
        });
    }
}
