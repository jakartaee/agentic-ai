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
package ee.jakarta.tck.ai.agent.framework.trace;

import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Internal utility for verifying the workflow execution flow.
 * Captures the sequence of lifecycle method calls.
 */
@ApplicationScoped
public class ExecutionTraceRecorder {

    /**
     * Represents the different phases of an agent's lifecycle.
     */
    public enum Phase {
        TRIGGER, DECISION, ACTION, OUTCOME, HANDLE_EXCEPTION
    }

    /**
     * Represents a single recorded trace entry.
     *
     * @param phase the lifecycle phase
     * @param methodName the name of the method called
     * @param args the arguments passed to the method
     * @param at the timestamp of the recording
     */
    public record TraceEntry(Phase phase, String methodName, Object[] args, Instant at) {}

    private final List<TraceEntry> entries = new CopyOnWriteArrayList<>();

    /**
     * Records an execution trace entry.
     *
     * @param phase the lifecycle phase
     * @param methodName the name of the method
     * @param args the method arguments
     */
    public void record(Phase phase, String methodName, Object... args) {
        entries.add(new TraceEntry(phase, methodName, args, Instant.now()));
    }

    /**
     * Returns all recorded trace entries.
     *
     * @return the list of trace entries
     */
    public List<TraceEntry> entries() {
        return List.copyOf(entries);
    }

    /**
     * Returns the sequence of recorded phases.
     *
     * @return the list of phases
     */
    public List<Phase> phases() {
        return entries.stream().map(TraceEntry::phase).toList();
    }

    /**
     * Resets the recorded traces.
     */
    public void reset() {
        entries.clear();
    }

    /**
     * Asserts that the recorded phases match the expected order.
     *
     * @param expected the expected sequence of phases
     * @throws AssertionError if the order does not match
     */
    public void assertOrder(Phase... expected) {
        List<Phase> actual = phases();
        List<Phase> expectedList = List.of(expected);
        if (!Objects.equals(actual, expectedList)) {
            throw new AssertionError("Expected phase order " + expectedList + " but got " + actual);
        }
    }
}
