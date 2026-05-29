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

import jakarta.ai.agent.LargeLanguageModel;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

import java.time.Instant;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Spec-faithful CDI stub for {@link LargeLanguageModel} used in TCK behavioral tests.
 *
 * <p>Enforces the spec contract: null-prompt/resultType → IAE, strict arity on varargs
 * overloads, JSON-B serialization of parameters and deserialization of typed responses.
 * Responses and failures are queued and consumed in FIFO order.</p>
 */
@ApplicationScoped
public class LargeLanguageModelStub implements LargeLanguageModel {

    private final Jsonb jsonb = JsonbBuilder.create();
    private final Queue<Object> scriptedResponses = new ConcurrentLinkedQueue<>();
    private final Queue<RuntimeException> scriptedFailures = new ConcurrentLinkedQueue<>();
    private final List<RecordedCall> calls = new CopyOnWriteArrayList<>();

    /**
     * Releases the underlying {@link Jsonb} instance when the CDI context shuts down.
     * Yasson does not hold OS-level resources today, but the spec contract is to close it.
     */
    @PreDestroy
    void close() {
        try {
            jsonb.close();
        } catch (Exception ignored) {
            // best-effort — the bean is going away anyway
        }
    }

    /**
     * Records a call to the model. {@code prompt} is the raw template; {@code effectivePrompt}
     * is the post-substitution text (equals {@code prompt} for single-arg overloads).
     */
    public record RecordedCall(String prompt, String effectivePrompt, Class<?> resultType,
                               Object[] params, int overload, Instant at) {}

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
        calls.add(new RecordedCall(prompt, effectivePrompt, resultType, params, overload, Instant.now()));

        RuntimeException failure = scriptedFailures.poll();
        if (failure != null) throw failure;

        Object next = scriptedResponses.poll();
        if (next == null) {
            throw new IllegalStateException(
                    "No scripted response queued for prompt: '" + prompt + "'. "
                  + "Call enqueueResponse(...) before invoking the LLM in this test.");
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
        String result = prompt;
        for (Object p : params) {
            result = result.replaceFirst(java.util.regex.Pattern.quote("{}"),
                     java.util.regex.Matcher.quoteReplacement(jsonb.toJson(p)));
        }
        return result;
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
            } catch (jakarta.json.bind.JsonbException e) {
                throw new IllegalArgumentException("type conversion failed", e);
            }
        }
        throw new IllegalArgumentException("Cannot convert " + next.getClass() + " to " + resultType);
    }

    @Override
    public <T> T unwrap(Class<T> implClass) {
        if (implClass != null && implClass.isInstance(this)) {
            return implClass.cast(this);
        }
        throw new IllegalArgumentException("Cannot unwrap to " + implClass);
    }
}