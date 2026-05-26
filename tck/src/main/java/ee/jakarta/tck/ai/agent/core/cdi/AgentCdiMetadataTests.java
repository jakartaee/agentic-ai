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
package ee.jakarta.tck.ai.agent.core.cdi;

import ee.jakarta.tck.ai.agent.framework.junit.anno.Assertion;
import ee.jakarta.tck.ai.agent.framework.junit.anno.Standalone;
import jakarta.ai.agent.Agent;

import static org.assertj.core.api.Assertions.assertThat;

@Standalone
public class AgentCdiMetadataTests {

    @Agent(name = "customAgent", description = "A test agent for BHV-002")
    private static class NamedAgentFixture {}

    @Assertion(id = "AGENTICAI-CDI-BHV-002",
               section = "Architecture, Convention over Configuration",
               strategy = "@Agent.name() and @Agent.description() are retained and readable at runtime via "
                        + "reflection; a class annotated @Agent(name=\"customAgent\", description=...) returns "
                        + "those values from getAnnotation(Agent.class)")
    public void agentNameAndDescriptionAttributesAreAccessibleAtRuntime() {
        Agent annotation = NamedAgentFixture.class.getAnnotation(Agent.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.name()).isEqualTo("customAgent");
        assertThat(annotation.description()).isEqualTo("A test agent for BHV-002");
    }
}
