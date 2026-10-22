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
 * Resolved sender identity: the customer's own values where supplied, with
 * anything missing pulled out of the message by the agent.
 */
public record Identity(String name, String email, String company) {

    /** The JSON shape a model reply must have to bind to this record. */
    public static final String SCHEMA =
            "Return ONLY valid JSON of the form "
            + "{\"name\":\"...\",\"email\":\"...\",\"company\":\"...\"}.";
}
