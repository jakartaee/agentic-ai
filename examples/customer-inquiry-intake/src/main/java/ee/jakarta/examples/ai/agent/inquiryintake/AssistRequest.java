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
 * CDI event that triggers the {@link AssistAgent} workflow. The agent handles
 * four distinct flows and the concrete subtype of the event selects which
 * one: drafting the customer's message from a brief, refining the current
 * message, classifying the message into an inquiry type, or extracting the
 * sender's identity from some text. One agent, one place that talks to the
 * LLM.
 */
public sealed interface AssistRequest {

    /** Draft a polished message from a short customer brief. */
    record Draft(String brief) implements AssistRequest {}

    /** Refine an existing message with a chat instruction. */
    record Refine(String currentMessage, String instruction) implements AssistRequest {}

    /** Classify a message into one of the configured inquiry types. */
    record Classify(String message) implements AssistRequest {}

    /** Pull the sender's name, email, and company out of a block of text. */
    record Extract(String text) implements AssistRequest {}
}
