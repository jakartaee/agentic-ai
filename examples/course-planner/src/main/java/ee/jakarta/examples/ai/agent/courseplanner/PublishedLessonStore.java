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

import ee.jakarta.examples.ai.agent.courseplanner.model.Lesson;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Holds the latest published lesson.
 */
@ApplicationScoped
public class PublishedLessonStore {

    private final AtomicReference<Lesson> current = new AtomicReference<>();

    public void publish(Lesson lesson) {
        current.set(lesson);
    }

    public Lesson current() {
        return current.get();
    }
}
