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

import jakarta.enterprise.context.RequestScoped;
import jakarta.json.JsonObject;

/**
 * Per-request holder for the result the {@link AssistAgent} just produced.
 * The agent writes into this; the resource reads from it after
 * {@code trigger.fire(...)} returns, in the same HTTP request.
 */
@RequestScoped
public class AssistResponse {

    private String message;
    private String inquiryType;
    private JsonObject identity;

    public String message()        { return message; }
    public String inquiryType()    { return inquiryType; }
    public JsonObject identity()   { return identity; }

    public void setMessage(String message)         { this.message = message; }
    public void setInquiryType(String inquiryType) { this.inquiryType = inquiryType; }
    public void setIdentity(JsonObject identity)   { this.identity = identity; }
}
