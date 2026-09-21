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
import jakarta.ai.agent.HandleException;
import jakarta.ai.agent.Trigger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.ConstraintViolationException;

/**
 * Agent exercising Jakarta Validation integration.
 *
 * <p>The {@code @Trigger} always runs and produces an {@link InputData} carrying
 * the event payload. The {@code @Action} receives it with {@code @Valid}, so the
 * runtime validates the object <em>before</em> invoking the action body. A
 * {@code null} payload violates {@link InputData}'s {@code @NotNull} constraint,
 * raising a {@link ConstraintViolationException} that is routed to the
 * {@code @HandleException} method instead of running the action body.</p>
 */
@Agent
@ApplicationScoped
public class ValidatedAgent {

    @Inject
    ExecutionTraceRecorder trace;

    @Trigger
    public InputData onEvent(@Observes ValidationEvent event) {
        trace.record(Phase.TRIGGER, "onEvent", event);
        return new InputData(event.payload());
    }

    @Action
    public void act(@Valid InputData data) {
        trace.record(Phase.ACTION, "act", data);
    }

    @HandleException
    public void onInvalid(ConstraintViolationException ex) {
        trace.record(Phase.HANDLE_EXCEPTION, "onInvalid", ex);
    }
}