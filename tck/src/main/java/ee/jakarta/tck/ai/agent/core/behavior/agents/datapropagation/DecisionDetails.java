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
package ee.jakarta.tck.ai.agent.core.behavior.agents.datapropagation;

/**
 * Domain object carried as the {@code details} of a {@link jakarta.ai.agent.Result}
 * returned by {@link ResultDetailsAgent}. When non-{@code null}, {@code details}
 * becomes part of workflow state and must be injectable by its runtime type into
 * later phases.
 *
 * @param value a distinguishable payload asserted by the later phase
 */
public record DecisionDetails(String value) {}