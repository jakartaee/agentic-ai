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
package ee.jakarta.tck.ai.agent.core.behavior.agents.concurrency;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link ConcurrencyCoordinator}.
 *
 * <p>These exercise the TCK's own concurrency harness, not the specification: they
 * confirm the coordinator's barrier and leak-detection logic behaves correctly, so
 * the {@code @RequiresImplementation} concurrency assertions are trustworthy when a
 * compatible implementation finally runs them. Tagged {@code internal} so they do
 * not inflate spec assertion counts.</p>
 */
@Tag("internal")
public class ConcurrencyCoordinatorTests {

    private static final int WORKFLOWS = 8;

    /**
     * Simulates correctly isolated {@code @WorkflowScoped} instances: each thread
     * keeps its own marker across the barrier, so no leak is reported.
     */
    @Test
    void reportsNoMismatchWhenEachWorkflowKeepsItsOwnState() throws Exception {
        ConcurrencyCoordinator coordinator = new ConcurrencyCoordinator();
        coordinator.arm(WORKFLOWS);

        runConcurrently(WORKFLOWS, marker -> {
            // A per-thread holder stands in for an isolated @WorkflowScoped instance.
            String isolated = marker;
            coordinator.awaitAllStarted();
            coordinator.recordObservation(marker, isolated);
        });

        assertEquals(WORKFLOWS, coordinator.observationCount());
        assertTrue(coordinator.mismatches().isEmpty(),
                "isolated workflows must not report any leak: " + coordinator.mismatches());
    }

    /**
     * Simulates a thread-unsafe shared instance: all threads write to one holder
     * before reading it back, so at least one workflow observes another's marker
     * and the coordinator reports a leak.
     */
    @Test
    void detectsMismatchWhenStateLeaksAcrossWorkflows() throws Exception {
        ConcurrencyCoordinator coordinator = new ConcurrencyCoordinator();
        coordinator.arm(WORKFLOWS);
        AtomicReference<String> shared = new AtomicReference<>();

        runConcurrently(WORKFLOWS, marker -> {
            // A single shared holder stands in for a leaking, shared instance.
            shared.set(marker);
            coordinator.awaitAllStarted();
            coordinator.recordObservation(marker, shared.get());
        });

        assertEquals(WORKFLOWS, coordinator.observationCount());
        assertFalse(coordinator.mismatches().isEmpty(),
                "a shared instance must produce at least one detected leak");
    }

    private void runConcurrently(int count, MarkerTask task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(count);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                String marker = "wf-" + i;
                futures.add(pool.submit(() -> task.run(marker)));
            }
            for (Future<?> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }
    }

    @FunctionalInterface
    private interface MarkerTask {
        void run(String marker);
    }
}