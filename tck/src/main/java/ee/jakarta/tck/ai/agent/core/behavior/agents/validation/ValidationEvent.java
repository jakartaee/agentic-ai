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

/**
 * Triggering event for the validation fixtures. The {@code payload} carries the
 * value the {@link ValidatedAgent} turns into an {@link InputData} to be
 * validated downstream: a non-{@code null} payload produces valid input, a
 * {@code null} payload produces input that fails {@code @NotNull} validation.
 */
public record ValidationEvent(String payload) {
}