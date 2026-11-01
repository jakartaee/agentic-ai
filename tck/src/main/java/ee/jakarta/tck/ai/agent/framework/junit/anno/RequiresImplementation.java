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

import org.junit.jupiter.api.Tag;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an assertion that only a compatible implementation can satisfy, one
 * that needs the {@code @Decision}, {@code @Action}, {@code @Outcome} or
 * {@code @OnException} phases dispatched, or an agent rejected at deployment.
 *
 * <p>The specification repository, which has no implementation, excludes the
 * {@code requires-implementation} tag to run the remainder on plain CDI. A
 * compatibility run excludes nothing.</p>
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Tag("requires-implementation")
public @interface RequiresImplementation {
}
