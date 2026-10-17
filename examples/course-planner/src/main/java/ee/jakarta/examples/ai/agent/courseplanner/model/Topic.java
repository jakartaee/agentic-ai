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
package ee.jakarta.examples.ai.agent.courseplanner.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * The event that triggers {@code LessonAgent}: a topic the teacher wants a
 * lesson on, with the material to base it on.
 */
public record Topic(
        @NotBlank String subject,
        @NotBlank String title,
        @NotBlank @Size(min = 80, message = "paste at least 80 characters of material") String material) {
}
