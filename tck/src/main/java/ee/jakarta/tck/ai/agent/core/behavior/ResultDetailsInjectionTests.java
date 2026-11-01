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

import ee.jakarta.tck.ai.agent.core.behavior.agents.datapropagation.DecisionDetails;
import ee.jakarta.tck.ai.agent.core.behavior.agents.datapropagation.ResultDetailsAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.datapropagation.ResultDetailsEvent;
import ee.jakarta.tck.ai.agent.framework.junit.anno.Assertion;
import ee.jakarta.tck.ai.agent.framework.junit.anno.Deployed;
import ee.jakarta.tck.ai.agent.framework.junit.anno.RequiresImplementation;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * A non-{@code null} {@code Result.details} becomes part of
 * workflow state and is injectable by its runtime type into later phases.
 *
 * <p>Uses {@code @TestInstance(PER_CLASS)} with inline {@code trace.reset()} because
 * {@code @BeforeEach} does not run reliably between methods of a {@code @Deployed}
 * class inside the container.</p>
 */
@Deployed
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ResultDetailsInjectionTests {

    @Deployment
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "resultdetails.war")
                .addClasses(ResultDetailsAgent.class, ResultDetailsEvent.class, DecisionDetails.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject Event<ResultDetailsEvent> events;
    @Inject ExecutionTraceRecorder trace;

    @RequiresImplementation
    @Assertion(id = "AGENTICAI-DATA-006",
               section = "parameter-resolution",
               strategy = "A non-null Result.details is injectable by its runtime type into a later phase")
    public void resultDetailsIsInjectableByRuntimeType() {
        trace.reset();
        events.fire(new ResultDetailsEvent("input"));

        assertEquals(List.of(Phase.TRIGGER, Phase.DECISION, Phase.ACTION), trace.phases());

        Object[] actionArgs = trace.entries().get(2).args();
        assertInstanceOf(DecisionDetails.class, actionArgs[0]);
        assertEquals("details:input", ((DecisionDetails) actionArgs[0]).value());
    }
}