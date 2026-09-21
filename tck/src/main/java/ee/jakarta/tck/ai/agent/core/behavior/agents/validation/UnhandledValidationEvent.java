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
 * Triggering event for {@link UnhandledValidatedAgent}, which declares no
 * {@code @HandleException} method. A {@code null} payload produces a validation
 * failure that must propagate to the container.
 */
public record UnhandledValidationEvent(String payload) {
}