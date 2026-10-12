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
package ee.jakarta.examples.ai.agent.inquiryintake;

import jakarta.enterprise.context.RequestScoped;

/**
 * Per-request holder for the {@link IntakeRecord} the pipeline produced.
 */
@RequestScoped
public class IntakeOutput {

    private IntakeRecord record;

    public IntakeRecord record() {
        return record;
    }

    public void setRecord(IntakeRecord record) {
        this.record = record;
    }
}
