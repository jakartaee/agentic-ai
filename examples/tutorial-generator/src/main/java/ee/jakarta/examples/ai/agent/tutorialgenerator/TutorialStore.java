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
 * Holds the latest guide content produced by {@link TutorialAgent} so the
 * synchronous REST call can read it back after the event fires.
 *
 * <p>{@code @ApplicationScoped} is intentional: the guide must survive across
 * HTTP requests so a subsequent {@code /refine} call can read what a prior
 * {@code /generate} call produced. {@code volatile} provides the visibility
 * guarantee needed for the single-writer (agent action), single-reader
 * (resource) access pattern of this demo.
 */
@ApplicationScoped
public class TutorialStore {

    private volatile String html = "";

    public void put(String html) {
        this.html = html == null ? "" : html;
    }

    public String get() {
        return html;
    }
}
