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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link LargeLanguageModelStub}.
 *
 * <p>These exercise the TCK's own infrastructure, not the specification.
 * They are tagged {@code internal} so they do not inflate spec assertion
 * counts when running with {@code -Dgroups=standalone}.</p>
 */
@Tag("internal")
public class LargeLanguageModelStubTests {

    private LargeLanguageModelStub stub;

    @BeforeEach
    void setUp() {
        stub = new LargeLanguageModelStub();
    }

    @AfterEach
    void tearDown() {
        if (stub != null) {
            stub.close();
        }
        WorkflowContext.clear();
    }

    @Test
    void testEnqueueResponse() {
        stub.enqueueResponse("Hello");
        assertEquals("Hello", stub.query("test"));
    }

    @Test
    void testMultipleResponses() {
        stub.enqueueResponse("First");
        stub.enqueueResponse("Second");
        assertEquals("First", stub.query("1"));
        assertEquals("Second", stub.query("2"));
    }

    @Test
    void testNoResponseQueuedFailsLoudly() {
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> stub.query("test"));
        assertTrue(e.getMessage().contains("test"), "Message should reference the offending prompt");
    }

    @Test
    void testTypeMismatchThrowsLlmException() {
        stub.enqueueResponse(42);
        assertThrows(jakarta.ai.agent.LLMException.class, () -> stub.query("test", java.util.Date.class),
                "Spec contract: response type conversion failures must be LLMException");
    }

    @Test
    void testMultipleFailures() {
        stub.failWith(new RuntimeException("Error 1"));
        stub.failWith(new RuntimeException("Error 2"));
        stub.enqueueResponse("ok");

        RuntimeException e1 = assertThrows(RuntimeException.class, () -> stub.query("test 1"));
        assertEquals("Error 1", e1.getMessage());

        RuntimeException e2 = assertThrows(RuntimeException.class, () -> stub.query("test 2"));
        assertEquals("Error 2", e2.getMessage());

        assertEquals("ok", stub.query("test 3"), "Should consume queued response after failures are drained");
    }

    @Test
    void testRecordedCalls() {
        stub.enqueueResponse("r1");
        stub.enqueueResponse(7);
        stub.enqueueResponse("r3");
        stub.enqueueResponse("r4");

        stub.query("Hello 1");
        stub.query("Hello 2", Integer.class);
        stub.query("Hello 3", "p1");
        stub.query("Hello 4", String.class, "p2");

        List<LargeLanguageModelStub.RecordedCall> calls = stub.recordedCalls();
        assertEquals(4, calls.size());

        assertEquals(1, calls.get(0).overload());
        assertNull(calls.get(0).resultType());

        assertEquals(2, calls.get(1).overload());
        assertEquals(Integer.class, calls.get(1).resultType());

        assertEquals(3, calls.get(2).overload());
        assertNull(calls.get(2).resultType());
        assertArrayEquals(new Object[]{"p1"}, calls.get(2).params());

        assertEquals(4, calls.get(3).overload());
        assertEquals(String.class, calls.get(3).resultType());
        assertArrayEquals(new Object[]{"p2"}, calls.get(3).params());
    }

    @Test
    void testUnwrap() {
        assertSame(stub, stub.unwrap(LargeLanguageModelStub.class));
        assertThrows(IllegalArgumentException.class, () -> stub.unwrap(String.class));
    }

    @Test
    void testAutoCloseable() {
        LargeLanguageModelStub local = new LargeLanguageModelStub();
        local.enqueueResponse("hi");
        try (local) {
            assertEquals("hi", local.query("q"));
        }
        assertDoesNotThrow(local::close, "close() must be idempotent");
    }

    @Test
    void testPlaceholderNotCorruptedByBraceLikeParameter() {
        stub.enqueueResponse("ok");
        // Empty Map serializes to "{}"; a replaceFirst loop would then substitute
        // the next param into that injected text instead of the second placeholder.
        assertEquals("ok", stub.query("a {} b {} c", Map.of(), "X"));
        assertEquals("a {} b \"X\" c", stub.lastCall().effectivePrompt());
    }

    @Test
    void testRecordedCallsIsolatedPerWorkflow() {
        stub.enqueueResponse("a");
        stub.enqueueResponse("b");

        WorkflowContext.run("wf-1", () -> stub.query("prompt-1"));
        WorkflowContext.run("wf-2", () -> stub.query("prompt-2"));

        assertEquals(2, stub.recordedCalls().size(), "aggregate API still sees all calls");
        assertEquals(1, stub.recordedCallsForWorkflow("wf-1").size());
        assertEquals("prompt-1", stub.recordedCallsForWorkflow("wf-1").get(0).prompt());
        assertEquals(1, stub.recordedCallsForWorkflow("wf-2").size());
        assertEquals("prompt-2", stub.recordedCallsForWorkflow("wf-2").get(0).prompt());
        assertTrue(stub.recordedCallsForWorkflow("missing").isEmpty());

        assertEquals(List.of("prompt-1", "a"), stub.conversationHistoryForWorkflow("wf-1"));
        assertEquals(List.of("prompt-2", "b"), stub.conversationHistoryForWorkflow("wf-2"));

        assertThrows(IllegalArgumentException.class, () -> stub.recordedCallsForWorkflow(null));
        assertThrows(IllegalArgumentException.class, () -> stub.conversationHistoryForWorkflow(null));

        stub.reset();
        assertTrue(stub.recordedCalls().isEmpty());
        assertTrue(stub.recordedCallsForWorkflow("wf-1").isEmpty());
        assertTrue(stub.conversationHistoryForWorkflow("wf-1").isEmpty());
    }

    @Test
    void testConcurrentWorkflowsDoNotLeak() throws Exception {
        int threads = 8;
        int callsPerThread = 50;
        for (int i = 0; i < threads * callsPerThread; i++) {
            stub.enqueueResponse("ok");
        }

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicReference<Throwable> error = new AtomicReference<>();

        try {
            for (int t = 0; t < threads; t++) {
                final String workflowId = "wf-" + t;
                pool.submit(() -> {
                    try {
                        start.await();
                        WorkflowContext.run(workflowId, () -> {
                            for (int i = 0; i < callsPerThread; i++) {
                                String prompt = "turn for {} #" + i;
                                stub.query(prompt, workflowId);
                            }
                        });
                    } catch (Throwable e) {
                        error.compareAndSet(null, e);
                    } finally {
                        done.countDown();
                    }
                });
            }

            start.countDown();
            assertTrue(done.await(60, TimeUnit.SECONDS), "workers did not finish in time");
        } finally {
            pool.shutdown();
            if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
                pool.shutdownNow();
                assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS),
                        "executor did not terminate after shutdownNow()");
            }
        }

        assertNull(error.get(), () -> "worker failed: " + error.get());
        assertEquals(threads * callsPerThread, stub.recordedCalls().size());

        for (int t = 0; t < threads; t++) {
            String workflowId = "wf-" + t;
            List<LargeLanguageModelStub.RecordedCall> workflowCalls =
                    stub.recordedCallsForWorkflow(workflowId);
            assertEquals(callsPerThread, workflowCalls.size(), workflowId);
            String expectedFragment = "\"" + workflowId + "\"";
            for (LargeLanguageModelStub.RecordedCall call : workflowCalls) {
                assertTrue(call.effectivePrompt().contains(expectedFragment),
                        () -> "cross-workflow leak in effective prompt: " + call.effectivePrompt());
                assertFalse(call.effectivePrompt().matches(".*\"wf-[0-9]+\".*\"wf-[0-9]+\".*"),
                        () -> "unexpected double workflow token: " + call.effectivePrompt());
            }
            List<String> history = stub.conversationHistoryForWorkflow(workflowId);
            assertEquals(callsPerThread * 2, history.size(), workflowId + " conversation size");
        }
    }
}
