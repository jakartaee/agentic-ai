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

import java.util.List;

/**
 * A web form described to the agent: a title, a short intro, and its fields.
 *
 * @param title  form title
 * @param intro  short description shown above the form
 * @param fields the form fields
 */
public record FormSpec(String title, String intro, List<FieldSpec> fields) {
}
