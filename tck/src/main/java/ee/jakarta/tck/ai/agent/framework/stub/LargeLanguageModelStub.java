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
package ee.jakarta.tck.ai.agent.framework.stub;

import jakarta.ai.agent.LLMException;
import jakarta.ai.agent.LargeLanguageModel;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The {@link LargeLanguageModel} the TCK deploys into every test archive.
 *
 * <p>Tests script responses with {@link #enqueueResponse(Object)} or a failure
 * with {@link #failWith(RuntimeException)}; both are consumed in call order
 * from one FIFO shared by all workflows. Every call is recorded for assertion
 * through {@link #recordedCalls()} and {@link #lastCall()}. The stub enforces
 * the specification contract: null prompt or result type is an
 * {@code IllegalArgumentException}, placeholder and parameter counts must
 * match, parameters are serialized and typed responses deserialized with
 * JSON Binding.</p>
 */
@ApplicationScoped
public class LargeLanguageModelStub implements LargeLanguageModel {

    private final Jsonb jsonb = JsonbBuilder.create();
    private final Object dispatchLock = new Object();
    private final Queue<Object> scriptedResponses = new ConcurrentLinkedQueue<>();
    private final Queue<RuntimeException> scriptedFailures = new ConcurrentLinkedQueue<>();
    private final List<RecordedCall> calls = new CopyOnWriteArrayList<>();

    @PreDestroy
    void destroy() {
        try {
            jsonb.close();
        } catch (Exception ignored) {
            // the bean is going away anyway
        }
    }

    /**
     * Records a call to the model. {@code prompt} is the raw template; {@code effectivePrompt}
     * is the post-substitution text (equals {@code prompt} for single-arg overloads).
     */
    public record RecordedCall(String prompt, String effectivePrompt, Class<?> resultType,
                               Object[] params, int overload) {}

    public void enqueueResponse(Object response) {
        scriptedResponses.add(response);
    }

    public void failWith(RuntimeException e) {
        scriptedFailures.add(e);
    }

    public void reset() {
        scriptedResponses.clear();
        scriptedFailures.clear();
        calls.clear();
    }

    public List<RecordedCall> recordedCalls() {
        return List.copyOf(calls);
    }

    public RecordedCall lastCall() {
        return calls.isEmpty() ? null : calls.get(calls.size() - 1);
    }

    @Override
    public String query(String prompt) {
        return dispatch(prompt, null, 1);
    }

    @Override
    public <T> T query(String prompt, Class<T> resultType) {
        if (resultType == null) throw new IllegalArgumentException("resultType is null");
        return dispatch(prompt, resultType, 2);
    }

    @Override
    public String query(String prompt, Object... parameters) {
        return dispatch(prompt, null, 3, parameters);
    }

    @Override
    public <T> T query(String prompt, Class<T> resultType, Object... parameters) {
        if (resultType == null) throw new IllegalArgumentException("resultType is null");
        return dispatch(prompt, resultType, 4, parameters);
    }

    private <T> T dispatch(String prompt, Class<T> resultType, int overload, Object... params) {
        if (prompt == null) throw new IllegalArgumentException("prompt is null");

        String effectivePrompt = (overload >= 3) ? applyPlaceholders(prompt, params) : prompt;

        final Object next;
        // Recording the call and taking the next scripted item must be one step,
        // or concurrent workflows could pair a call with another's response.
        synchronized (dispatchLock) {
            calls.add(new RecordedCall(prompt, effectivePrompt, resultType, params, overload));

            RuntimeException failure = scriptedFailures.poll();
            if (failure != null) {
                throw failure;
            }

            next = scriptedResponses.poll();
            if (next == null) {
                throw new IllegalStateException(
                        "No scripted response queued for prompt: '" + prompt + "'. "
                      + "Call enqueueResponse(...) before invoking the LLM in this test.");
            }
        }

        return convert(next, resultType);
    }

    private String applyPlaceholders(String prompt, Object[] params) {
        int n = countExactBraces(prompt);
        if (n >= 1 && params.length != n) {
            throw new IllegalArgumentException(
                "placeholder/parameter arity mismatch: " + n + " vs " + params.length);
        }
        if (n == 0 && params.length > 1) {
            throw new IllegalArgumentException(
                "prompt has no placeholder but received " + params.length + " parameters");
        }
        if (n == 0) {
            // single structured context param — keep raw prompt as effective text
            return prompt;
        }

        // Walk {} positions in the ORIGINAL prompt so injected JSON (e.g. "{}")
        // cannot steal later substitutions.
        StringBuilder out = new StringBuilder(prompt.length());
        int from = 0;
        for (int i = 0; i < n; i++) {
            int brace = prompt.indexOf("{}", from);
            out.append(prompt, from, brace);
            out.append(toJson(params[i]));
            from = brace + 2;
        }
        out.append(prompt, from, prompt.length());
        return out.toString();
    }

    private String toJson(Object value) {
        try {
            return jsonb.toJson(value);
        } catch (JsonbException e) {
            throw new IllegalArgumentException("parameter cannot be serialized to JSON", e);
        }
    }

    private int countExactBraces(String prompt) {
        int count = 0;
        int idx = 0;
        while ((idx = prompt.indexOf("{}", idx)) != -1) {
            count++;
            idx += 2;
        }
        return count;
    }

    @SuppressWarnings("unchecked")
    private <T> T convert(Object next, Class<T> resultType) {
        if (resultType == null)          return (T) next.toString();
        if (resultType.isInstance(next)) return (T) next;
        if (resultType == String.class)  return (T) next.toString();
        if (next instanceof String json) {
            try {
                return jsonb.fromJson(json, resultType);
            } catch (JsonbException e) {
                throw new LLMException("type conversion failed", e);
            }
        }
        throw new LLMException("Cannot convert " + next.getClass() + " to " + resultType);
    }

    @Override
    public <T> T unwrap(Class<T> implClass) {
        if (implClass != null && implClass.isInstance(this)) {
            return implClass.cast(this);
        }
        throw new IllegalArgumentException("Cannot unwrap to " + implClass);
    }
}
