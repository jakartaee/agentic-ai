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
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * CDI-enabled stub for {@link LargeLanguageModel} used in TCK behavioral tests.
 * Provides a way to script responses and verify received prompts.
 *
 * <p>Responses and failures are queued and consumed in FIFO order.</p>
 */
@ApplicationScoped
public class LargeLanguageModelStub implements LargeLanguageModel {

    private final Queue<Object> scriptedResponses = new ArrayDeque<>();
    private final Queue<RuntimeException> scriptedFailures = new ArrayDeque<>();
    private final List<RecordedCall> calls = new CopyOnWriteArrayList<>();

    /**
     * Records a call to the model.
     *
     * @param prompt the prompt sent to the model
     * @param resultType the expected result type, or null for default String queries
     * @param params the parameters sent to the model
     * @param overload the overload index (1-4)
     * @param at the timestamp of the call
     */
    public record RecordedCall(String prompt, Class<?> resultType, Object[] params, int overload, Instant at) {}

    /**
     * Enqueues a response to be returned by the next call to any query method.
     *
     * @param response the response object
     */
    public void enqueueResponse(Object response) {
        scriptedResponses.add(response);
    }

    /**
     * Enqueues a failure to be thrown on the next call to any query method.
     *
     * @param e the exception to throw
     */
    public void failWith(RuntimeException e) {
        this.scriptedFailures.add(e);
    }

    /**
     * Resets the stub's state (scripted responses, failures, and recorded calls).
     */
    public void reset() {
        scriptedResponses.clear();
        scriptedFailures.clear();
        calls.clear();
    }

    /**
     * Returns a list of all recorded calls.
     *
     * @return the list of recorded calls
     */
    public List<RecordedCall> recordedCalls() {
        return List.copyOf(calls);
    }

    /**
     * Returns the last recorded call, or null if no calls were made.
     *
     * @return the last recorded call
     */
    public RecordedCall lastCall() {
        return calls.isEmpty() ? null : calls.get(calls.size() - 1);
    }

    @Override
    public String query(String prompt) {
        return dispatch(prompt, null, 1);
    }

    @Override
    public <T> T query(String prompt, Class<T> resultType) {
        return dispatch(prompt, resultType, 2);
    }

    @Override
    public String query(String prompt, Object... parameters) {
        return dispatch(prompt, null, 3, parameters);
    }

    @Override
    public <T> T query(String prompt, Class<T> resultType, Object... parameters) {
        return dispatch(prompt, resultType, 4, parameters);
    }

    @SuppressWarnings("unchecked")
    private <T> T dispatch(String prompt, Class<T> resultType, int overload, Object... params) {
        if (prompt == null) {
            throw new IllegalArgumentException("prompt is null");
        }
        calls.add(new RecordedCall(prompt, resultType, params, overload, Instant.now()));

        RuntimeException scriptedFailure = scriptedFailures.poll();
        if (scriptedFailure != null) {
            throw scriptedFailure;
        }

        Object next = scriptedResponses.poll();
        if (next == null) {
            throw new IllegalStateException(
                    "No scripted response queued for prompt: '" + prompt + "'. "
                  + "Call enqueueResponse(...) before invoking the LLM in this test.");
        }

        if (resultType != null && !resultType.isInstance(next)) {
            // For String result type, fall back to toString() conversion of the queued value.
            if (resultType == String.class) {
                return (T) next.toString();
            }
            // Spec contract: type conversion failures are reported as IllegalArgumentException
            // (see LargeLanguageModel.query Javadoc).
            throw new IllegalArgumentException(
                    "Cannot convert queued response of type " + next.getClass().getName()
                  + " to requested result type " + resultType.getName());
        }

        return (T) (resultType == null ? next.toString() : next);
    }

    @Override
    public <T> T unwrap(Class<T> implClass) {
        if (implClass != null && implClass.isInstance(this)) {
            return implClass.cast(this);
        }
        throw new IllegalArgumentException("Cannot unwrap to " + implClass);
    }
}
