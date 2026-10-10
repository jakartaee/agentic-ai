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
 * CDI event that triggers the {@link AssistAgent} workflow.
 * <p>
 * On a first draft the request carries only {@code brief} (what the customer
 * roughly wants to say) and the agent writes a polished message. On refine,
 * the request carries {@code currentMessage} plus an {@code instruction} and
 * the agent revises the existing message. The current message is passed on
 * every refine turn so the model edits the real artifact rather than relying
 * on conversational memory alone.
 *
 * @param brief          what the customer wants to say, or {@code null} on refine
 * @param instruction    the chat refinement instruction, or {@code null} on first draft
 * @param currentMessage the message to revise, or {@code null} on first draft
 */
public record AssistRequest(String brief, String instruction, String currentMessage) {

    /** True when the request carries an existing message to revise. */
    public boolean refine() {
        return currentMessage != null && !currentMessage.isBlank();
    }
}
