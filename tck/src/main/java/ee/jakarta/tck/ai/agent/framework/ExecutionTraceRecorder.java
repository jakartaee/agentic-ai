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
package ee.jakarta.tck.ai.agent.framework;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Records every phase invocation of a workflow, so a test can check which
 * phases ran, in what order, and with which arguments.
 */
@ApplicationScoped
public class ExecutionTraceRecorder {

    public enum Phase {
        TRIGGER,
        DECISION,
        ACTION,
        OUTCOME,
        ON_EXCEPTION
    }

    /** One recorded invocation: the phase, the method that ran, and the arguments it received. */
    public record TraceEntry(Phase phase, String methodName, Object[] args) {}

    private final List<TraceEntry> entries = new CopyOnWriteArrayList<>();

    public void record(Phase phase, String methodName, Object... args) {
        entries.add(new TraceEntry(phase, methodName, args));
    }

    public List<TraceEntry> entries() {
        return List.copyOf(entries);
    }

    /** The recorded entries of the given phase, in the order they were recorded. */
    public List<TraceEntry> entries(Phase phase) {
        return entries.stream().filter(entry -> entry.phase() == phase).toList();
    }

    /**
     * The first recorded entry of the given phase.
     *
     * @throws AssertionError if no entry of that phase was recorded
     */
    public TraceEntry entry(Phase phase) {
        return entries(phase).stream()
                .findFirst()
                .orElseThrow(() -> new AssertionError("No " + phase + " entry was recorded. The trace is " + phases()));
    }

    public List<Phase> phases() {
        return entries.stream().map(TraceEntry::phase).toList();
    }

    /** The names of the methods that ran, in the order they were recorded. */
    public List<String> methodNames() {
        return entries.stream().map(TraceEntry::methodName).toList();
    }

    public void reset() {
        entries.clear();
    }
}
