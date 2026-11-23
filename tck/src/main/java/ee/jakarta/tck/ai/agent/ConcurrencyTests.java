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
package ee.jakarta.tck.ai.agent;

import ee.jakarta.tck.ai.agent.framework.TckModel;
import jakarta.ai.agent.Action;
import jakarta.ai.agent.Agent;
import jakarta.ai.agent.LargeLanguageModel;
import jakarta.ai.agent.Outcome;
import jakarta.ai.agent.Trigger;
import jakarta.ai.agent.WorkflowScoped;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that concurrent workflow executions of an {@code @ApplicationScoped}
 * model bean and a {@code @WorkflowScoped} agent remain isolated from one another.
 *
 * <p>Several workflows are fired from a thread pool and rendezvous at a
 * {@link ConcurrencyCoordinator} barrier so their
 * agent instances are all alive simultaneously, maximizing the chance that a
 * thread-unsafe implementation leaks state. What the tests check is
 * deterministic. Regardless of interleaving, every workflow must observe only its
 * own marker, and the recorded model calls must be exactly the per-workflow prompts
 * with no loss or corruption.</p>
 */
@ExtendWith(ArquillianExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ConcurrencyTests {

    private static final int WORKFLOWS = 8;
    private static final long RUN_TIMEOUT_SECONDS = 60;

    @Deployment
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "concurrency.war")
                .addClasses(
                        ConcurrentIsolationAgent.class,
                        ConcurrencyEvent.class,
                        ConcurrencyCoordinator.class,
                        ConcurrencyCoordinator.LlmObservation.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject
    Event<ConcurrencyEvent> events;

    @Inject
    ConcurrencyCoordinator coordinator;

    /**
     * Each concurrently fired event carries a unique {@code marker} used to detect state
     * leakage between overlapping workflow executions.
     *
     * @param marker a value unique to this workflow execution
     */
    public record ConcurrencyEvent(String marker) {}

    /**
     * The trigger stores the event's unique marker in an instance field and then
     * waits at a shared barrier so every concurrent workflow's instance is alive at
     * the same time. If the implementation correctly gives each workflow its own
     * instance, the marker read back in {@code @Outcome} always matches the marker the
     * workflow was triggered with; a shared or thread-unsafe instance would let one
     * workflow overwrite another's field. The action additionally sends the
     * per-workflow marker to the injected {@link LargeLanguageModel} and the outcome
     * reads the conversation back, so the echoed turn and the conversation history
     * can be checked for cross-workflow corruption.
     */
    @Agent
    @WorkflowScoped
    public static class ConcurrentIsolationAgent {

        @Inject
        LargeLanguageModel llm;

        @Inject
        ConcurrencyCoordinator coordinator;

        private String marker;
        private String echoed;

        @Trigger
        public void onEvent(@Observes ConcurrencyEvent event) {
            this.marker = event.marker();
            coordinator.awaitAllStarted();
        }

        @Action
        public void queryModel(ConcurrencyEvent event) {
            echoed = llm.query("turn:{}", marker);
        }

        @Outcome
        public void finish(ConcurrencyEvent event) {
            coordinator.recordObservation(event.marker(), this.marker);
            coordinator.recordLlm(event.marker(), echoed, llm.query(TckModel.HISTORY));
        }
    }

    /**
     * Application-scoped coordinator that lets a set of concurrent workflow executions
     * rendezvous so their agent instances are guaranteed to be alive at the same time,
     * then records whether each workflow observed only its own state and what the
     * model echoed back to it.
     *
     * <p>The rendezvous is a {@link CyclicBarrier}. Every workflow parks in its
     * {@code @Trigger} until all of them have started, which maximizes the window in
     * which a thread-unsafe implementation could leak state across instances. The
     * barrier also fails fast (via a timeout recorded as a mismatch) if the
     * implementation dispatches workflows sequentially instead of concurrently.</p>
     */
    @ApplicationScoped
    public static class ConcurrencyCoordinator {

        private static final long AWAIT_TIMEOUT_SECONDS = 30;

        /**
         * What one workflow saw from the model.
         *
         * @param echoed the response to the workflow's own turn
         * @param history the conversation the model reported afterwards
         */
        public record LlmObservation(String echoed, String history) {}

        private final List<String> mismatches = new CopyOnWriteArrayList<>();
        private final AtomicInteger observations = new AtomicInteger();
        private final Map<String, LlmObservation> llmObservations = new ConcurrentHashMap<>();
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
            llmObservations.clear();
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
         * Records what the model echoed to one workflow and the conversation it
         * reported for that workflow.
         *
         * @param marker the marker this workflow was triggered with
         * @param echoed the response to the workflow's turn
         * @param history the conversation history the model returned
         */
        public void recordLlm(String marker, String echoed, String history) {
            llmObservations.put(marker, new LlmObservation(echoed, history));
        }

        /** Each workflow's model observation, keyed by its marker. */
        public Map<String, LlmObservation> llmObservations() {
            return Map.copyOf(llmObservations);
        }

        /** The number of observations recorded since the coordinator was last armed. */
        public int observationCount() {
            return observations.get();
        }

        /** A description of every detected state leak or barrier failure, empty when there was none. */
        public List<String> mismatches() {
            return List.copyOf(mismatches);
        }
    }

    /**
     * Concurrent {@code @WorkflowScoped} executions each observe only their own workflow state.
     *
     * <p>Specification section {@code threading-and-concurrency}.
     */
    @Test
    public void workflowStateIsIsolatedAcrossConcurrentWorkflows() throws Exception {
        coordinator.arm(WORKFLOWS);
        fireConcurrently(WORKFLOWS);

        assertEquals(WORKFLOWS, coordinator.observationCount());
        assertEquals(List.of(), coordinator.mismatches());
    }

    /**
     * Concurrent workflows drive the shared facade without corrupting each other's turns. Each
     * gets its own turn echoed back and a conversation holding only that turn.
     *
     * <p>Specification section {@code llm-conversational-state}.
     */
    @Test
    public void llmCallsAreNotCorruptedAcrossConcurrentWorkflows() throws Exception {
        coordinator.arm(WORKFLOWS);
        fireConcurrently(WORKFLOWS);

        Map<String, ConcurrencyCoordinator.LlmObservation> observed = coordinator.llmObservations();
        assertEquals(WORKFLOWS, observed.size());
        for (int i = 0; i < WORKFLOWS; i++) {
            String turn = "turn:\"" + marker(i) + "\"";
            ConcurrencyCoordinator.LlmObservation observation = observed.get(marker(i));
            assertEquals(turn, observation.echoed(), "Echoed turn of " + marker(i));
            assertEquals(turn, observation.history(), "Conversation of " + marker(i));
        }
    }

    private static String marker(int index) {
        return "wf-" + index;
    }

    private void fireConcurrently(int count) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(count);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                String marker = marker(i);
                futures.add(pool.submit(() -> events.fire(new ConcurrencyEvent(marker))));
            }
            for (Future<?> future : futures) {
                future.get(RUN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }
    }
}
