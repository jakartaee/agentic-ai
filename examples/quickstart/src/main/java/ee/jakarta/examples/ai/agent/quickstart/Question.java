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

/**
 * CDI event that triggers the {@link QuestionAgent} workflow. Intentionally has
 * no Bean Validation constraints so a blank question reaches {@code @Decision}
 * and demonstrates early workflow termination via {@code Result(false, ...)}.
 */
public record Question(String text) {
}
