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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ExecutionTraceRecorder}.
 *
 * <p>These exercise the TCK's own infrastructure, not the specification.
 * They are tagged {@code internal} so they do not inflate spec assertion
 * counts when running with {@code -Dgroups=standalone}.</p>
 */
@Tag("internal")
public class ExecutionTraceRecorderTests {

    private ExecutionTraceRecorder recorder;

    @BeforeEach
    void setUp() {
        recorder = new ExecutionTraceRecorder();
    }

    @Test
    void testRecordAndGet() {
        recorder.record(ExecutionTraceRecorder.Phase.TRIGGER, "onEvent", "arg1");
        recorder.record(ExecutionTraceRecorder.Phase.ACTION, "doWork");

        List<ExecutionTraceRecorder.TraceEntry> entries = recorder.entries();
        assertEquals(2, entries.size());
        assertEquals(ExecutionTraceRecorder.Phase.TRIGGER, entries.get(0).phase());
        assertEquals("onEvent", entries.get(0).methodName());
        assertArrayEquals(new Object[]{"arg1"}, entries.get(0).args());

        assertEquals(ExecutionTraceRecorder.Phase.ACTION, entries.get(1).phase());
        assertEquals("doWork", entries.get(1).methodName());
    }

    @Test
    void testAssertOrderSuccess() {
        recorder.record(ExecutionTraceRecorder.Phase.TRIGGER, "m1");
        recorder.record(ExecutionTraceRecorder.Phase.ACTION, "m2");
        recorder.record(ExecutionTraceRecorder.Phase.OUTCOME, "m3");

        assertDoesNotThrow(() -> recorder.assertOrder(
                ExecutionTraceRecorder.Phase.TRIGGER,
                ExecutionTraceRecorder.Phase.ACTION,
                ExecutionTraceRecorder.Phase.OUTCOME
        ));
    }

    @Test
    void testAssertOrderFailure() {
        recorder.record(ExecutionTraceRecorder.Phase.TRIGGER, "m1");
        recorder.record(ExecutionTraceRecorder.Phase.OUTCOME, "m3");

        assertThrows(AssertionError.class, () -> recorder.assertOrder(
                ExecutionTraceRecorder.Phase.TRIGGER,
                ExecutionTraceRecorder.Phase.ACTION,
                ExecutionTraceRecorder.Phase.OUTCOME
        ));
    }

    @Test
    void testReset() {
        recorder.record(ExecutionTraceRecorder.Phase.TRIGGER, "m1");
        recorder.reset();
        assertTrue(recorder.entries().isEmpty());
    }
}
