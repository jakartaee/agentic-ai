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
package ee.jakarta.examples.ai.agent.quickstart;

import jakarta.validation.constraints.NotBlank;

/**
 * CDI event that triggers the {@link QuestionAgent} workflow.
 * <p>
 * The constraint is what {@code @Valid} on the trigger parameter enforces: an
 * agent is never started with a blank question. {@link AskResource} rejects
 * blank input with a 400 before firing, so the constraint guards the agent
 * against any other caller.
 */
public record Question(@NotBlank String text) {
}
