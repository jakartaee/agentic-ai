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

import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Application-scoped coordinator that lets a set of concurrent workflow executions
 * rendezvous so their agent instances are guaranteed to be alive at the same time,
 * then records whether each workflow observed only its own state.
 *
 * <p>The rendezvous is a {@link CyclicBarrier}: every workflow parks in its
 * {@code @Trigger} until all of them have started, which maximizes the window in
 * which a thread-unsafe implementation could leak state across instances. The
 * barrier also fails fast (via a timeout recorded as a mismatch) if the
 * implementation dispatches workflows sequentially instead of concurrently.</p>
 */
@ApplicationScoped
public class ConcurrencyCoordinator {

    private static final long AWAIT_TIMEOUT_SECONDS = 30;

    private final List<String> mismatches = new CopyOnWriteArrayList<>();
    private final AtomicInteger observations = new AtomicInteger();
    private volatile CyclicBarrier barrier;

    /**
     * Prepares the coordinator for a run of {@code parties} concurrent workflows.
     *
     * @param parties the number of workflows expected to rendezvous
     */
    public void arm(int parties) {
        barrier = new CyclicBarrier(parties);
        mismatches.clear();
        observations.set(0);
    }

    /**
     * Blocks until every armed workflow has reached this point, so all agent
     * instances coexist. A timeout or broken barrier is recorded as a mismatch
     * (for example when the implementation runs workflows sequentially).
     */
    public void awaitAllStarted() {
        CyclicBarrier current = barrier;
        if (current == null) {
            return;
        }
        try {
            current.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            mismatches.add("interrupted while awaiting the barrier");
        } catch (BrokenBarrierException | TimeoutException e) {
            mismatches.add("barrier not reached by all workflows: " + e);
        }
    }

    /**
     * Records one workflow's observation of its own state.
     *
     * @param expected the marker this workflow was triggered with
     * @param observed the marker read back from the agent instance's state
     */
    public void recordObservation(String expected, String observed) {
        observations.incrementAndGet();
        if (!Objects.equals(expected, observed)) {
            mismatches.add("state leaked: expected=" + expected + " observed=" + observed);
        }
    }

    /**
     * @return the number of recorded observations
     */
    public int observationCount() {
        return observations.get();
    }

    /**
     * @return descriptions of any detected state leaks or barrier failures
     */
    public List<String> mismatches() {
        return List.copyOf(mismatches);
    }
}