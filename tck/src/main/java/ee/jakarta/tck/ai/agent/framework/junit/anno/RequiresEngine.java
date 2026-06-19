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

import ee.jakarta.tck.ai.agent.framework.junit.extensions.EngineCondition;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a TCK assertion that can only be satisfied when a Reference
 * Implementation of the Agentic AI engine is present to dispatch the
 * {@code @Decision}, {@code @Action} and {@code @Outcome} phases after the
 * {@code @Trigger}.
 *
 * <p>The {@link EngineCondition} detects engine presence automatically at
 * runtime (no configuration required): such tests run when a Reference
 * Implementation is deployed and are skipped against the plain-CDI baseline
 * (see {@link RequiresNoEngine}).</p>
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(EngineCondition.class)
public @interface RequiresEngine {
}
