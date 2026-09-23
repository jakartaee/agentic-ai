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
package ee.jakarta.tck.ai.agent.core.behavior;

import ee.jakarta.tck.ai.agent.core.behavior.agents.concurrency.ConcurrencyCoordinator;
import ee.jakarta.tck.ai.agent.core.behavior.agents.concurrency.ConcurrencyEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.concurrency.ConcurrentIsolationAgent;
import ee.jakarta.tck.ai.agent.framework.junit.anno.Assertion;
import ee.jakarta.tck.ai.agent.framework.junit.anno.Deployed;
import ee.jakarta.tck.ai.agent.framework.junit.anno.RequiresImplementation;
import ee.jakarta.tck.ai.agent.framework.stub.LargeLanguageModelStub;
import ee.jakarta.tck.ai.agent.framework.stub.LargeLanguageModelStub.RecordedCall;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.TestInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that concurrent workflow executions of an {@code @ApplicationScoped}
 * model bean and a {@code @WorkflowScoped} agent remain isolated from one another.
 *
 * <p>This is the real concurrency exercise that supersedes the earlier sequential
 * "v1 proxy" (see {@code AGENTICAI-LLM-BHV-007}). Several workflows are fired from
 * a thread pool and rendezvous at a {@link ConcurrencyCoordinator} barrier so their
 * agent instances are all alive simultaneously, maximizing the chance that a
 * thread-unsafe implementation leaks state. The assertions themselves are
 * deterministic: regardless of interleaving, every workflow must observe only its
 * own marker, and the recorded model calls must be exactly the per-workflow prompts
 * with no loss or corruption.</p>
 *
 * <p><strong>Scope limitation:</strong> the reference
 * {@link LargeLanguageModelStub} keys its per-workflow conversation history by the
 * TCK's {@code WorkflowContext} thread-local fixture, which a compatible
 * implementation does not populate. Cross-workflow conversation-history isolation
 * is therefore asserted here through the implementation-agnostic global call log
 * (no corruption across concurrent calls) rather than through per-workflow history
 * lookups.</p>
 */
@Deployed
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ConcurrencyIsolationTests {

    private static final int WORKFLOWS = 8;
    private static final long RUN_TIMEOUT_SECONDS = 60;

    @Deployment
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "concurrencyisolation.war")
                .addClasses(ConcurrentIsolationAgent.class, ConcurrencyEvent.class,
                            ConcurrencyCoordinator.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject Event<ConcurrencyEvent> events;
    @Inject ConcurrencyCoordinator coordinator;
    @Inject LargeLanguageModelStub stub;

    @RequiresImplementation
    @Assertion(id = "AGENTICAI-CONCURRENCY-001",
               section = "cdi-integration",
               strategy = "Concurrent @WorkflowScoped executions each observe only their own workflow state")
    public void workflowStateIsIsolatedAcrossConcurrentWorkflows() throws Exception {
        stub.reset();
        coordinator.arm(WORKFLOWS);
        enqueueResponses(WORKFLOWS);

        fireConcurrently(WORKFLOWS);

        assertThat(coordinator.observationCount()).isEqualTo(WORKFLOWS);
        assertThat(coordinator.mismatches()).isEmpty();
    }

    @RequiresImplementation
    @Assertion(id = "AGENTICAI-CONCURRENCY-002",
               section = "llm-conversational-state",
               strategy = "Concurrent workflows drive the shared model without losing or corrupting each other's calls")
    public void llmCallsAreNotCorruptedAcrossConcurrentWorkflows() throws Exception {
        stub.reset();
        coordinator.arm(WORKFLOWS);
        enqueueResponses(WORKFLOWS);

        fireConcurrently(WORKFLOWS);

        List<String> prompts = stub.recordedCalls().stream()
                .map(RecordedCall::effectivePrompt)
                .toList();
        String[] expected = IntStream.range(0, WORKFLOWS)
                .mapToObj(i -> "turn:\"" + marker(i) + "\"")
                .toArray(String[]::new);
        assertThat(prompts).containsExactlyInAnyOrder(expected);
    }

    private void enqueueResponses(int count) {
        for (int i = 0; i < count; i++) {
            stub.enqueueResponse("ok");
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