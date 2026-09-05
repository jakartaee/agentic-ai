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

import ee.jakarta.tck.ai.agent.framework.workflow.WorkflowContext;
import jakarta.ai.agent.LLMException;
import jakarta.ai.agent.LargeLanguageModel;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Spec-faithful CDI stub for {@link LargeLanguageModel} used in TCK behavioral tests.
 *
 * <p>Enforces the spec contract: null-prompt/resultType → IAE, strict arity on varargs
 * overloads, JSON-B serialization of parameters and deserialization of typed responses.
 * Scripted responses and failures are a single global FIFO channel by design (not keyed
 * per workflow); fixtures enqueue before concurrent work and consume in call order.
 * Per-workflow isolation covers recorded calls and conversation history only.</p>
 *
 * <p>Recorded calls and conversation history are keyed by
 * {@link WorkflowContext#current()} so concurrent workflows remain isolated even
 * though this bean is {@code @ApplicationScoped}. Placeholder substitution walks
 * {@code {}} positions in the original prompt so injected JSON cannot steal later
 * substitutions (the historical corruption root cause was {@code replaceFirst} on
 * the running result, not JSON-B). The shared {@link Jsonb} is accessed under
 * synchronization on the instance itself: JSON-B requires thread-safe {@code Jsonb}
 * implementations, so the lock is defensive and also shares a monitor with
 * {@link #close()}.</p>
 */
@ApplicationScoped
public class LargeLanguageModelStub implements LargeLanguageModel, AutoCloseable {

    private final Jsonb jsonb = JsonbBuilder.create();
    private final Object dispatchLock = new Object();
    private final Queue<Object> scriptedResponses = new ConcurrentLinkedQueue<>();
    private final Queue<RuntimeException> scriptedFailures = new ConcurrentLinkedQueue<>();
    private final List<RecordedCall> calls = new CopyOnWriteArrayList<>();
    private final Map<String, List<RecordedCall>> callsByWorkflow = new ConcurrentHashMap<>();
    private final Map<String, List<String>> conversationByWorkflow = new ConcurrentHashMap<>();
    private volatile boolean closed;

    /**
     * Releases the underlying {@link Jsonb} instance. Idempotent; safe for
     * try-with-resources and repeated calls. CDI shutdown delegates here via
     * {@link #destroy()}. Synchronized on {@code jsonb} so close cannot race
     * with {@code toJson}/{@code fromJson}.
     */
    @Override
    public void close() {
        synchronized (jsonb) {
            if (closed) {
                return;
            }
            closed = true;
            try {
                jsonb.close();
            } catch (Exception ignored) {
                // best-effort — the bean / test fixture is going away anyway
            }
        }
    }

    @PreDestroy
    void destroy() {
        close();
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
        callsByWorkflow.clear();
        conversationByWorkflow.clear();
    }

    public List<RecordedCall> recordedCalls() {
        return List.copyOf(calls);
    }

    /**
     * @param workflowId workflow id; must not be {@code null}
     * @return recorded calls for the given workflow id (empty when none)
     * @throws IllegalArgumentException if {@code workflowId} is {@code null}
     */
    public List<RecordedCall> recordedCallsForWorkflow(String workflowId) {
        requireWorkflowId(workflowId);
        List<RecordedCall> workflowCalls = callsByWorkflow.get(workflowId);
        return workflowCalls == null ? List.of() : List.copyOf(workflowCalls);
    }

    /**
     * Returns conversation turns for the given workflow as interleaved effective
     * prompts then responses. When a scripted failure is thrown after the prompt
     * is recorded, the prompt turn is intentionally left unpaired (no response
     * entry) — the call aborted before a model reply existed.
     *
     * @param workflowId workflow id; must not be {@code null}
     * @return conversation turns (empty when none)
     * @throws IllegalArgumentException if {@code workflowId} is {@code null}
     */
    public List<String> conversationHistoryForWorkflow(String workflowId) {
        requireWorkflowId(workflowId);
        List<String> history = conversationByWorkflow.get(workflowId);
        return history == null ? List.of() : List.copyOf(history);
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
        String workflowId = WorkflowContext.current();

        final List<String> conversation;
        final Object next;
        synchronized (dispatchLock) {
            RecordedCall call = new RecordedCall(prompt, effectivePrompt, resultType, params, overload, Instant.now());
            calls.add(call);
            callsByWorkflow
                    .computeIfAbsent(workflowId, id -> new CopyOnWriteArrayList<>())
                    .add(call);
            conversation = conversationByWorkflow
                    .computeIfAbsent(workflowId, id -> new CopyOnWriteArrayList<>());
            conversation.add(effectivePrompt);

            RuntimeException failure = scriptedFailures.poll();
            if (failure != null) {
                // Prompt turn recorded; no response appended — intentional unpaired turn.
                throw failure;
            }

            next = scriptedResponses.poll();
            if (next == null) {
                throw new IllegalStateException(
                        "No scripted response queued for prompt: '" + prompt + "'. "
                      + "Call enqueueResponse(...) before invoking the LLM in this test.");
            }
        }

        T result = convert(next, resultType);
        conversation.add(result == null ? "null" : result.toString());
        return result;
    }

    private static void requireWorkflowId(String workflowId) {
        if (workflowId == null) {
            throw new IllegalArgumentException("workflowId is null");
        }
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
            synchronized (jsonb) {
                return jsonb.toJson(value);
            }
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
                synchronized (jsonb) {
                    return jsonb.fromJson(json, resultType);
                }
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
