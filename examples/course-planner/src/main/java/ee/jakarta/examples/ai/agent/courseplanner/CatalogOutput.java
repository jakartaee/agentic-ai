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
package ee.jakarta.examples.ai.agent.courseplanner;

import ee.jakarta.examples.ai.agent.courseplanner.model.CatalogEntry;
import jakarta.enterprise.context.RequestScoped;

/**
 * Per-request holder for what {@code CatalogAgent} did with the lesson: the
 * entry it filed, or why it declined.
 */
@RequestScoped
public class CatalogOutput {

    private CatalogEntry filed;
    private String stopReason;

    public CatalogEntry filed() {
        return filed;
    }

    public void setFiled(CatalogEntry filed) {
        this.filed = filed;
    }

    public String stopReason() {
        return stopReason;
    }

    public void setStopReason(String stopReason) {
        this.stopReason = stopReason;
    }
}
