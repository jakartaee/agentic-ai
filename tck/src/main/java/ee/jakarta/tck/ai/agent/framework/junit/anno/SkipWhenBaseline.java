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
package ee.jakarta.tck.ai.agent.framework.junit.anno;

import ee.jakarta.tck.ai.agent.framework.junit.extensions.SkipWhenBaselineCondition;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Skips a test class when the plain-CDI baseline profile is active.
 *
 * <p>Used for deployment-error tests. Their {@code @ShouldThrowException}
 * check runs in the client JVM, before an in-container
 * {@code @RequiresImplementation} condition can skip the deployment.</p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(SkipWhenBaselineCondition.class)
public @interface SkipWhenBaseline {
}
