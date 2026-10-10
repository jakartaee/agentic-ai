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

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Holds the latest draft of the customer's message so the synchronous REST
 * call can read it back after the {@link AssistAgent} workflow completes.
 *
 * <p>{@code @ApplicationScoped} is intentional: the draft must survive across
 * HTTP requests so a later {@code /message/refine} call can read what the
 * earlier {@code /message/draft} call produced. {@code volatile} gives the
 * visibility guarantee needed for the single-writer (agent action),
 * single-reader (resource) access pattern of this demo.
 */
@ApplicationScoped
public class MessageStore {

    private volatile String message = "";

    public void put(String message) {
        this.message = message == null ? "" : message;
    }

    public String get() {
        return message;
    }
}
