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
package ee.jakarta.tck.ai.agent.core.behavior.agents.concurrency;

/**
 * Triggering event for {@link ConcurrentIsolationAgent}. Each concurrently fired
 * event carries a unique {@code marker} used to detect state leakage between
 * overlapping workflow executions.
 *
 * @param marker a value unique to this workflow execution
 */
public record ConcurrencyEvent(String marker) {}