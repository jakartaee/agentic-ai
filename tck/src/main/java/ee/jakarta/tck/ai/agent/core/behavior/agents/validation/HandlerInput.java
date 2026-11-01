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

import jakarta.validation.constraints.NotBlank;

/**
 * A domain object returned by a {@code @Trigger} and consumed only by an
 * {@code @OnException} method with {@code @Valid}. Return values are not
 * validated, so a blank {@code note} reaches the handler's parameter
 * validation untouched.
 */
public record HandlerInput(@NotBlank String note) {
}
