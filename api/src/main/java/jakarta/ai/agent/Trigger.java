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
 * Marks a method as an external trigger that initiates an agent workflow.
 * <p>
 * A trigger method initiates the agent's workflow, typically in response to a CDI event
 * or other external stimulus. The method may accept parameters that are added to the
 * workflow context.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Agent
 * public class FraudDetectionAgent {
 *
 *     @Trigger
 *     public void processTransaction(BankTransaction transaction) {
 *         // Workflow trigger logic - initiates the agent workflow
 *     }
 * }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Trigger {
}
