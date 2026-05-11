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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

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
    void testTypeMismatchThrowsIllegalArgumentException() {
        stub.enqueueResponse(42);
        assertThrows(IllegalArgumentException.class, () -> stub.query("test", java.util.Date.class),
                "Spec contract: type conversion failures must be IllegalArgumentException");
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
}
