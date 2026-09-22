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
 * Output type returned by more than one phase of {@link TieBreakAgent}, so that
 * parameter resolution must choose between two workflow-state objects of the same
 * type. The {@code marker} distinguishes which phase produced the instance.
 *
 * @param marker identifies the producing phase (e.g. {@code "decision"} or {@code "action"})
 */
public record SameTypeOutput(String marker) {}