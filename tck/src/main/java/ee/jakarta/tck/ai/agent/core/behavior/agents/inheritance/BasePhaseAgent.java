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
package ee.jakarta.tck.ai.agent.core.behavior.agents.inheritance;

import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder.Phase;
import jakarta.ai.agent.Action;
import jakarta.inject.Inject;

/**
 * Superclass (not itself an {@code @Agent}) that declares an annotated
 * {@code @Action} phase method. Agent subclasses inherit this method; whether it
 * participates in the workflow depends on whether the subclass overrides it.
 */
public class BasePhaseAgent {

    @Inject
    protected ExecutionTraceRecorder trace;

    @Action
    public void act() {
        trace.record(Phase.ACTION, "act");
    }
}