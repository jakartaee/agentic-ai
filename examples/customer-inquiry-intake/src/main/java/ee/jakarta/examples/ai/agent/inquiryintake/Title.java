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
package ee.jakarta.examples.ai.agent.inquiryintake;

/**
 * A short subject line drafted from the message, suitable for a ticketing
 * system. Returned by the {@code draftTitle} action and consumed by
 * {@code assemble} by type — a record wrapper (rather than a bare
 * {@code String}) so the runtime can match the parameter unambiguously
 * against the polished message, which is also a string.
 */
public record Title(String value) {
}
