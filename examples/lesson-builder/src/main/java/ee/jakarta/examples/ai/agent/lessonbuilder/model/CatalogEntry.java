/*****************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *****************************************************************************/
package ee.jakarta.examples.ai.agent.lessonbuilder.model;

import java.util.List;

/**
 * One entry in the course catalogue: the lesson's title and subject with the
 * metadata the agent extracted so another teacher can find and judge it.
 */
public record CatalogEntry(
        String id,
        String subject,
        String title,
        String summary,
        Coverage coverage,
        Estimate estimate,
        List<String> standards) {
}
