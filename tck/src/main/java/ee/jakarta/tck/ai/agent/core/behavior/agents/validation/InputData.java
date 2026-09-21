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
package ee.jakarta.tck.ai.agent.core.behavior.agents.validation;

import jakarta.validation.constraints.NotNull;

/**
 * A domain object produced by a phase and passed to a later phase for
 * validation. When injected with {@code @Valid}, the runtime cascades
 * validation into this object; a {@code null} {@code value} violates the
 * {@link NotNull} constraint and raises a
 * {@code jakarta.validation.ConstraintViolationException}.
 */
public record InputData(@NotNull String value) {
}