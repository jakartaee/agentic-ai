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
package ee.jakarta.examples.ai.agent.lessonbuilder;

import ee.jakarta.examples.ai.agent.lessonbuilder.model.CatalogEntry;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The course catalogue: every lesson that has been filed, in the order it
 * was filed.
 */
@ApplicationScoped
public class Catalog {

    private final List<CatalogEntry> entries = new CopyOnWriteArrayList<>();

    public void add(CatalogEntry entry) {
        entries.add(entry);
    }

    public List<CatalogEntry> entries() {
        return List.copyOf(entries);
    }
}
