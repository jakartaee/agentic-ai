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

import ee.jakarta.tck.ai.agent.framework.junit.extensions.ImplementationPresentCondition;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a baseline ("precondition") TCK assertion that holds only when no
 * compatible implementation is dispatching phases — i.e. plain CDI observes the
 * {@code @Trigger} and nothing else runs.
 *
 * <p>When a compatible implementation is present (detected automatically by
 * {@link ImplementationPresentCondition}) it also dispatches the downstream phases, so
 * these trigger-only assertions no longer hold and are superseded by the
 * corresponding {@link RequiresImplementation} behavior tests. They are therefore
 * skipped in implementation mode.</p>
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(ImplementationPresentCondition.class)
public @interface RequiresNoImplementation {
}
