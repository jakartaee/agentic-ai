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
package ee.jakarta.tck.ai.agent.behavior.agents.llm;

import jakarta.json.bind.JsonbException;
import jakarta.json.bind.annotation.JsonbTypeSerializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;

/**
 * A parameter whose JSON Binding serialization always fails.
 */
@JsonbTypeSerializer(NonSerializableParam.Serializer.class)
public class NonSerializableParam {

    public static class Serializer implements JsonbSerializer<NonSerializableParam> {
        @Override
        public void serialize(NonSerializableParam obj, JsonGenerator generator, SerializationContext ctx) {
            throw new JsonbException("intentional serialization failure");
        }
    }
}
