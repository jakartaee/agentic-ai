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
package ee.jakarta.examples.ai.agent.coursecontentstudio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bridges the agent's phase progress to the browser over Server-Sent Events.
 * <p>
 * The UI opens {@code GET /api/progress/{runId}} (an SSE stream) before it fires
 * the workflow, passing the same {@code runId} in the request. As each phase
 * runs, the agent calls {@link #publish} and the step is pushed live to the
 * popup; {@link #complete} closes the stream when the workflow ends.
 */
@ApplicationScoped
public class ProgressTracker {

    private record Channel(SseEventSink sink, Sse sse) {
    }

    private final Map<String, Channel> channels = new ConcurrentHashMap<>();

    /** Registers the SSE connection for a run. */
    public void register(String runId, SseEventSink sink, Sse sse) {
        if (runId != null && !runId.isBlank()) {
            channels.put(runId, new Channel(sink, sse));
        }
    }

    /** Pushes one progress step to the browser (no-op if nobody is listening). */
    public void publish(String runId, String message) {
        Channel channel = runId == null ? null : channels.get(runId);
        if (channel != null && !channel.sink().isClosed()) {
            try {
                channel.sink().send(channel.sse().newEvent("step", message));
            } catch (RuntimeException ignored) {
                // Browser went away mid-run; nothing to do.
            }
        }
    }

    /** Signals completion and closes the stream. */
    public void complete(String runId) {
        Channel channel = runId == null ? null : channels.remove(runId);
        if (channel != null && !channel.sink().isClosed()) {
            try {
                channel.sink().send(channel.sse().newEvent("done", "done"));
                channel.sink().close();
            } catch (RuntimeException ignored) {
                // Already closed.
            }
        }
    }
}
