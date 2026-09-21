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
package ee.jakarta.tck.ai.agent.core.behavior.agents.validation;

import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder.Phase;
import jakarta.ai.agent.Action;
import jakarta.ai.agent.Agent;
import jakarta.ai.agent.Trigger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.validation.Valid;

/**
 * Like {@link ValidatedAgent} but with no {@code @HandleException} method. When
 * validation fails, the {@code ConstraintViolationException} has no matching
 * handler and must propagate to the container.
 */
@Agent
@ApplicationScoped
public class UnhandledValidatedAgent {

    @Inject
    ExecutionTraceRecorder trace;

    @Trigger
    public InputData onEvent(@Observes UnhandledValidationEvent event) {
        trace.record(Phase.TRIGGER, "onEvent", event);
        return new InputData(event.payload());
    }

    @Action
    public void act(@Valid InputData data) {
        trace.record(Phase.ACTION, "act", data);
    }
}