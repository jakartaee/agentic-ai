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
 * Marks a method as the outcome (end) of an agent workflow.
 * <p>
 * An outcome method denotes the completion of the agent's workflow and may produce
 * a final result or side effect. This phase executes after actions have completed.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Agent
 * public class FraudDetectionAgent {
 *
 *     @Outcome
 *     public void finalizeTransaction(BankTransaction transaction) {
 *         // Mark transaction as processed
 *         transaction.setProcessed(true);
 *         entityManager.merge(transaction);
 *     }
 * }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Outcome {
}
