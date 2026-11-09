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
package ee.jakarta.tck.ai.agent.behavior;

import ee.jakarta.tck.ai.agent.behavior.agents.datapropagation.SameTypeOutput;
import ee.jakarta.tck.ai.agent.behavior.agents.datapropagation.SameTypeAgent;
import ee.jakarta.tck.ai.agent.behavior.agents.datapropagation.SameTypeEvent;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder.Phase;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * When more than one earlier phase has returned an object of the same type, a later phase must be
 * injected with the most recently returned instance.
 *
 * <p>Uses {@code @TestInstance(PER_CLASS)} with inline {@code trace.reset()} because
 * {@code @BeforeEach} does not run reliably between methods of an Arquillian test
 * class inside the container.</p>
 */
@ExtendWith(ArquillianExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ResolutionOrderTests {

    @Deployment
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "dataresolutionorder.war")
                .addClasses(SameTypeAgent.class, SameTypeEvent.class, SameTypeOutput.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject Event<SameTypeEvent> events;
    @Inject ExecutionTraceRecorder trace;
    /**
     * When two phases return the same type, the later phase is injected with the most recently
     * returned instance.
     *
     * <p>Specification section {@code parameter-resolution}.
     */
    @Test
    public void mostRecentlyReturnedObjectWins() {
        trace.reset();
        events.fire(new SameTypeEvent("input"));

        assertEquals(List.of(Phase.TRIGGER, Phase.DECISION, Phase.ACTION, Phase.OUTCOME), trace.phases());

        Object[] outcomeArgs = trace.entries().get(3).args();
        assertInstanceOf(SameTypeOutput.class, outcomeArgs[0]);
        assertEquals("action", ((SameTypeOutput) outcomeArgs[0]).marker());
    }
}