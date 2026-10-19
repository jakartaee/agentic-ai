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
package ee.jakarta.examples.ai.agent.courseplanner.model;

/**
 * The model's verdict on whether the lesson's content matches the subject
 * and title the teacher put on it.
 */
public record ContentCheck(boolean matches, String actualTopic, String reason) {
}
