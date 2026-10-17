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
 * The output of the {@link IntakeAgent} pipeline: resolved identity, chosen
 * category, drafted subject line, and the polished message. Shown to the
 * customer on the review screen before they confirm.
 */
public record IntakeRecord(Identity identity, Classification classification, String title, String message) {
}
