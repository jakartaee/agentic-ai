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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Records the sequence of workflow phase invocations for ordering assertions.
 */
@ApplicationScoped
public class ExecutionTraceRecorder {

    public enum Phase {
        TRIGGER, DECISION, ACTION, OUTCOME, ON_EXCEPTION
    }

    /**
     * @param phase the lifecycle phase
     * @param methodName the name of the method called
     * @param args the arguments passed to the method
     */
    public record TraceEntry(Phase phase, String methodName, Object[] args) {}

    private final List<TraceEntry> entries = new CopyOnWriteArrayList<>();

    public void record(Phase phase, String methodName, Object... args) {
        entries.add(new TraceEntry(phase, methodName, args));
    }

    public List<TraceEntry> entries() {
        return List.copyOf(entries);
    }

    public List<Phase> phases() {
        return entries.stream().map(TraceEntry::phase).toList();
    }

    public void reset() {
        entries.clear();
    }
}
