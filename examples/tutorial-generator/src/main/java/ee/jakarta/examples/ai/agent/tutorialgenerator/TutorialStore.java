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
package ee.jakarta.examples.ai.agent.tutorialgenerator;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Holds the latest guidance JSON produced by {@link TutorialAgent} so the
 * synchronous REST call can read it back after the event fires.
 *
 * <p>{@code @ApplicationScoped} is intentional: the guidance must survive
 * across HTTP requests so a later {@code /refine} call can read what an
 * earlier {@code /generate} call produced. {@code volatile} gives the
 * visibility guarantee needed for the single-writer (agent action),
 * single-reader (resource) access pattern of this demo.
 */
@ApplicationScoped
public class TutorialStore {

    private volatile String json = "";

    public void put(String json) {
        this.json = json == null ? "" : json;
    }

    public String get() {
        return json;
    }
}
