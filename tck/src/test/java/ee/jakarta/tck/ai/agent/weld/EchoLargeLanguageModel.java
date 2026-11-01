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
package ee.jakarta.tck.ai.agent.weld;

import ee.jakarta.tck.ai.agent.framework.llm.TckModel;
import jakarta.ai.agent.LLMException;
import jakarta.ai.agent.LargeLanguageModel;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;

import java.util.ArrayList;
import java.util.List;

/**
 * The specification repository's stand-in for an implementation's facade,
 * so the untagged assertions can run here on Weld Embedded. It implements
 * the facade contract of the API Javadoc over the {@link TckModel} echo
 * behaviour, keeping the conversation per thread because plain CDI has no
 * workflow context. It is never shipped in the TCK.
 */
@ApplicationScoped
public class EchoLargeLanguageModel implements LargeLanguageModel {

    private final Jsonb jsonb = JsonbBuilder.create();
    private final ThreadLocal<List<String>> conversation = ThreadLocal.withInitial(ArrayList::new);

    @PreDestroy
    void destroy() {
        try {
            jsonb.close();
        } catch (Exception ignored) {
        }
    }

    @Override
    public String query(String prompt) {
        requirePrompt(prompt);
        return send(prompt);
    }

    @Override
    public <T> T query(String prompt, Class<T> resultType) {
        requirePrompt(prompt);
        requireResultType(resultType);
        return convert(send(prompt), resultType);
    }

    @Override
    public String query(String prompt, Object... parameters) {
        requirePrompt(prompt);
        return send(render(prompt, parameters));
    }

    @Override
    public <T> T query(String prompt, Class<T> resultType, Object... parameters) {
        requirePrompt(prompt);
        requireResultType(resultType);
        return convert(send(render(prompt, parameters)), resultType);
    }

    @Override
    public <T> T unwrap(Class<T> implClass) {
        if (implClass != null && implClass.isInstance(this)) {
            return implClass.cast(this);
        }
        throw new IllegalArgumentException("Cannot unwrap to " + implClass);
    }

    private static void requirePrompt(String prompt) {
        if (prompt == null) {
            throw new IllegalArgumentException("prompt is null");
        }
    }

    private static void requireResultType(Class<?> resultType) {
        if (resultType == null) {
            throw new IllegalArgumentException("resultType is null");
        }
    }

    private String send(String rendered) {
        if (rendered.contains(TckModel.FAIL)) {
            throw new LLMException("model failure requested by " + TckModel.FAIL);
        }
        List<String> turns = conversation.get();
        if (rendered.equals(TckModel.HISTORY)) {
            return String.join("\n", turns);
        }
        turns.add(rendered);
        return rendered;
    }

    private String render(String prompt, Object[] params) {
        int placeholders = countPlaceholders(prompt);
        if (placeholders >= 1 && params.length != placeholders) {
            throw new IllegalArgumentException(
                    "placeholder/parameter arity mismatch: " + placeholders + " vs " + params.length);
        }
        if (placeholders == 0) {
            if (params.length > 1) {
                throw new IllegalArgumentException(
                        "prompt has no placeholder but received " + params.length + " parameters");
            }
            if (params.length == 1) {
                toJson(params[0]);
            }
            return prompt;
        }
        StringBuilder out = new StringBuilder(prompt.length());
        int from = 0;
        for (Object param : params) {
            int brace = prompt.indexOf("{}", from);
            out.append(prompt, from, brace).append(toJson(param));
            from = brace + 2;
        }
        return out.append(prompt, from, prompt.length()).toString();
    }

    private static int countPlaceholders(String prompt) {
        int count = 0;
        for (int idx = prompt.indexOf("{}"); idx != -1; idx = prompt.indexOf("{}", idx + 2)) {
            count++;
        }
        return count;
    }

    private String toJson(Object value) {
        try {
            return jsonb.toJson(value);
        } catch (JsonbException e) {
            throw new IllegalArgumentException("parameter cannot be serialized to JSON", e);
        }
    }

    private <T> T convert(String response, Class<T> resultType) {
        if (resultType == String.class) {
            return resultType.cast(response);
        }
        try {
            return jsonb.fromJson(response, resultType);
        } catch (JsonbException e) {
            throw new LLMException("response cannot be converted to " + resultType.getName(), e);
        }
    }
}
