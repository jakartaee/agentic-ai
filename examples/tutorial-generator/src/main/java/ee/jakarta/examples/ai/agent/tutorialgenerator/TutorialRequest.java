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
package ee.jakarta.examples.ai.agent.tutorialgenerator;

/**
 * CDI event that triggers the {@link TutorialAgent} workflow.
 * <p>
 * When {@code currentHtml} is {@code null}/blank the agent generates a fresh
 * tutorial; otherwise it revises {@code currentHtml} according to
 * {@code instruction} (the chat refinement loop). The current artifact is passed
 * explicitly each turn so the model edits the real HTML rather than relying on
 * conversational memory alone.
 *
 * @param formSpec    the form to explain
 * @param instruction the developer's refinement request, or {@code null} on first generation
 * @param currentHtml the tutorial to revise, or {@code null} on first generation
 */
public record TutorialRequest(FormSpec formSpec, String instruction, String currentHtml) {
}
