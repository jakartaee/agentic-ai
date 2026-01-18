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
package jakarta.ai.agent;

/**
 * Represents the result of a decision or workflow step in an agent.
 * <p>
 * Used to standardize workflow branching and outcome handling.
 * The 'success' flag indicates a positive or negative result, and 'details' can hold
 * additional information, such as error messages, domain objects, or context.
 */
public record Result(boolean success, Object details) {}
