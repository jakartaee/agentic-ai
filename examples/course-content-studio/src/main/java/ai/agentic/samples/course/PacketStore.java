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
package ai.agentic.samples.course;

import ai.agentic.samples.course.model.CoursePacket;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Holds the single "current" packet the teacher is authoring, and bridges the
 * synchronous CDI event (fired by the REST layer) to the HTTP response: the
 * agent writes the finished packet here in its {@code @Outcome}, and the
 * resource reads it back after {@code Event.fire(...)} returns.
 * <p>
 * This is a single-author demo, so one latest packet is enough. A multi-user
 * app would key packets by author/session.
 */
@ApplicationScoped
public class PacketStore {

    private final AtomicReference<CoursePacket> current = new AtomicReference<>();

    /** The agent publishes the finished (or refined) packet here. */
    public void publish(CoursePacket packet) {
        current.set(packet);
    }

    /** The latest packet, or {@code null} if nothing has been generated yet. */
    public CoursePacket current() {
        return current.get();
    }

    /** JSON of the current packet, used as the refine round-trip payload. */
    public String currentJson() {
        CoursePacket packet = current.get();
        return packet == null ? null : Json.instance().toJson(packet);
    }

    /** Marks the current packet as approved by the teacher. */
    public CoursePacket approve() {
        CoursePacket packet = current.get();
        if (packet != null) {
            packet.setApproved(true);
        }
        return packet;
    }
}
