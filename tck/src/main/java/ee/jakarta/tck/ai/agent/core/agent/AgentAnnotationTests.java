/*
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package ee.jakarta.tck.ai.agent.core.agent;

import ee.jakarta.tck.ai.agent.framework.junit.anno.Assertion;
import jakarta.ai.agent.Agent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TCK tests for the {@link Agent} annotation.
 *
 * <p>These tests verify that the @Agent annotation conforms to the
 * Jakarta Agentic AI 1.0 specification requirements.
 */
public class AgentAnnotationTests {

    @Assertion(id = "AGENTICAI-AGENT-001",
               strategy = "Verify @Agent annotation exists in the jakarta.ai.agent package")
    public void testAgentAnnotationExists() {
        assertNotNull(Agent.class,
                "@Agent annotation must exist in jakarta.ai.agent package");
    }

    @Assertion(id = "AGENTICAI-AGENT-002",
               strategy = "Verify @Agent annotation has RUNTIME retention policy")
    public void testAgentAnnotationRetention() {
        Retention retention = Agent.class.getAnnotation(Retention.class);
        assertNotNull(retention,
                "@Agent must have @Retention annotation");
        assertEquals(RetentionPolicy.RUNTIME, retention.value(),
                "@Agent must have RUNTIME retention policy");
    }

    @Assertion(id = "AGENTICAI-AGENT-003",
               strategy = "Verify @Agent annotation targets TYPE elements")
    public void testAgentAnnotationTarget() {
        Target target = Agent.class.getAnnotation(Target.class);
        assertNotNull(target,
                "@Agent must have @Target annotation");

        ElementType[] targets = target.value();
        assertEquals(1, targets.length,
                "@Agent must target exactly one element type");
        assertEquals(ElementType.TYPE, targets[0],
                "@Agent must target TYPE elements");
    }

    @Assertion(id = "AGENTICAI-AGENT-004",
               strategy = "Verify @Agent annotation is an annotation type")
    public void testAgentIsAnnotationType() {
        assertTrue(Agent.class.isAnnotation(),
                "@Agent must be an annotation type");
    }

    @Assertion(id = "AGENTICAI-AGENT-005",
               strategy = "Verify @Agent can be applied to a class")
    public void testAgentCanBeAppliedToClass() {
        @Agent
        class TestAgent {}

        Agent annotation = TestAgent.class.getAnnotation(Agent.class);
        assertNotNull(annotation,
                "@Agent annotation must be retrievable from annotated class");
    }
}
