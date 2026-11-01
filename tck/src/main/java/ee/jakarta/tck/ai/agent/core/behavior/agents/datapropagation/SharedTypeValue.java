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
package ee.jakarta.tck.ai.agent.core.behavior.agents.datapropagation;

import jakarta.enterprise.context.Dependent;

/**
 * A type that is both a CDI bean (dependent-scoped, no-args constructor) and a
 * workflow-state object returned by a phase. Parameter resolution must prefer
 * the phase result, whose {@code marker} names the producing phase, over a
 * container-created instance, whose {@code marker} is {@code null}.
 */
@Dependent
public class SharedTypeValue {

    private final String marker;

    public SharedTypeValue() {
        this(null);
    }

    public SharedTypeValue(String marker) {
        this.marker = marker;
    }

    public String marker() {
        return marker;
    }
}
