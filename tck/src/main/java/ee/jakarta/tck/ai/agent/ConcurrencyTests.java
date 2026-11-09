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

import ee.jakarta.tck.ai.agent.agents.concurrency.ConcurrencyCoordinator;
import ee.jakarta.tck.ai.agent.agents.concurrency.ConcurrencyCoordinator.LlmObservation;
import ee.jakarta.tck.ai.agent.agents.concurrency.ConcurrencyEvent;
import ee.jakarta.tck.ai.agent.agents.concurrency.ConcurrentIsolationAgent;
import jakarta.enterprise.event.Event;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
                .addClasses(ConcurrentIsolationAgent.class, ConcurrencyEvent.class,
                            ConcurrencyCoordinator.class, LlmObservation.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject Event<ConcurrencyEvent> events;
    @Inject ConcurrencyCoordinator coordinator;
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

        Map<String, LlmObservation> observed = coordinator.llmObservations();
        assertEquals(WORKFLOWS, observed.size());
        for (int i = 0; i < WORKFLOWS; i++) {
            String turn = "turn:\"" + marker(i) + "\"";
            LlmObservation observation = observed.get(marker(i));
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