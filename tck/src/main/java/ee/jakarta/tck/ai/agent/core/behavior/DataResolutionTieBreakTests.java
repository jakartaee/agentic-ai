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

import ee.jakarta.tck.ai.agent.core.behavior.agents.datapropagation.SameTypeOutput;
import ee.jakarta.tck.ai.agent.core.behavior.agents.datapropagation.TieBreakAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.datapropagation.TieBreakEvent;
import ee.jakarta.tck.ai.agent.framework.junit.anno.Assertion;
import ee.jakarta.tck.ai.agent.framework.junit.anno.Deployed;
import ee.jakarta.tck.ai.agent.framework.junit.anno.RequiresImplementation;
import ee.jakarta.tck.ai.agent.framework.junit.anno.RequiresNoImplementation;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder.Phase;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.TestInstance;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * When more than one earlier phase has returned an object of the same type, a later phase must be
 * injected with the most recently returned instance.
 *
 * <p>Uses {@code @TestInstance(PER_CLASS)} with inline {@code trace.reset()} because
 * {@code @BeforeEach} does not run reliably between methods of a {@code @Deployed}
 * class inside the container.</p>
 */
@Deployed
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DataResolutionTieBreakTests {

    @Deployment
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "datatiebreak.war")
                .addClasses(TieBreakAgent.class, TieBreakEvent.class, SameTypeOutput.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject Event<TieBreakEvent> events;
    @Inject ExecutionTraceRecorder trace;

    @RequiresNoImplementation
    @Assertion(id = "AGENTICAI-DATA-005-PRECONDITION",
               section = "parameter-resolution",
               strategy = "Trigger fires and records its phase before the implementation dispatches further phases")
    public void triggerIsObserved() {
        trace.reset();
        events.fire(new TieBreakEvent("input"));
        assertThat(trace.phases()).containsExactly(Phase.TRIGGER);
    }

    @RequiresImplementation
    @Assertion(id = "AGENTICAI-DATA-005",
               section = "parameter-resolution",
               strategy = "When two phases return the same type, the later phase is injected with the most recently returned instance")
    public void mostRecentlyReturnedObjectWins() {
        trace.reset();
        events.fire(new TieBreakEvent("input"));

        assertThat(trace.phases())
                .containsExactly(Phase.TRIGGER, Phase.DECISION, Phase.ACTION, Phase.OUTCOME);

        Object[] outcomeArgs = trace.entries().get(3).args();
        assertThat(outcomeArgs[0]).isInstanceOf(SameTypeOutput.class);
        assertThat(((SameTypeOutput) outcomeArgs[0]).marker()).isEqualTo("action");
    }
}