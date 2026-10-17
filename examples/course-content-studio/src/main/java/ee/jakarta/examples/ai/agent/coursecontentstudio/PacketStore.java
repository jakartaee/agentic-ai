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
package ee.jakarta.examples.ai.agent.coursecontentstudio;

import ee.jakarta.examples.ai.agent.coursecontentstudio.model.CoursePacket;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Holds the packet the teacher is currently authoring.
 */
@ApplicationScoped
public class PacketStore {

    private final AtomicReference<CoursePacket> current = new AtomicReference<>();

    public void publish(CoursePacket packet) {
        current.set(packet);
    }

    public CoursePacket current() {
        return current.get();
    }

    public CoursePacket approve() {
        CoursePacket packet = current.get();
        if (packet != null) {
            packet.setApproved(true);
        }
        return packet;
    }
}
