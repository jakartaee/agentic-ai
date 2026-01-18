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
package jakarta.ai.agent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as an action in an agent workflow.
 *
 * <p>Actions perform operations as part of the agent's workflow execution.
 * They typically execute after decision methods determine the workflow should proceed.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Agent
 * public class FraudDetectionAgent {
 *
 *     @Action
 *     public void handleFraud(Fraud fraud, BankTransaction transaction) {
 *         if (fraud.isSerious()) {
 *             alertBankSecurity(fraud);
 *         }
 *         Customer customer = getCustomer(transaction);
 *         alertCustomer(fraud, transaction, customer);
 *     }
 * }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Action {
}
