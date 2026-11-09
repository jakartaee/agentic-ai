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
package ee.jakarta.tck.ai.agent.agents.llm;

import jakarta.ai.agent.LargeLanguageModel;

import java.util.function.Function;

/**
 * Carries one call to make against the facade from inside a workflow.
 *
 * @param probe the call; its return value, or the exception it throws, is
 *              recorded as the trigger's trace argument
 */
public record LlmProbeEvent(Function<LargeLanguageModel, Object> probe) {}
