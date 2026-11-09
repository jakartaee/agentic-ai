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
package ee.jakarta.tck.ai.agent.framework;

/**
 * The tokens the model behind the implementation's {@code LargeLanguageModel}
 * must recognise during a TCK run. The required behaviour is defined in the
 * TCK Reference Guide under "Additional Certification Requirements"; these
 * constants exist so the tests, the fixtures and the guide name the same
 * strings.
 */
public final class TckModel {

    /** A rendered prompt containing this token fails at the model. */
    public static final String FAIL = "[tck:fail]";

    /** A rendered prompt equal to this token returns the conversation so far. */
    public static final String HISTORY = "[tck:history]";

    private TckModel() {
    }
}
