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
 * <p>
 * Actions perform operations as part of the agent's workflow execution.
 * They execute after decision methods determine the workflow should proceed,
 * and receive the results from decision phases. Actions typically perform the
 * primary work of the agent, such as persisting data, calling external services,
 * or updating system state.
 * <p>
 * <b>Parameters</b><br>
 * Action methods can have the following types of parameters that will be
 * automatically resolved:
 * <ul>
 *   <li>Workflow state domain objects - Any objects from previous workflow phases,
 *       particularly decision results or trigger event objects</li>
 *   <li>Decision results - Objects returned from {@link Decision @Decision} methods
 *       (when using the Object or {@link Result} return patterns)</li>
 *   <li>{@link WorkflowContext} - The current workflow context for state management</li>
 *   <li>{@link LargeLanguageModel} - LLM instance for analysis or content generation</li>
 *   <li>Any other CDI injectable dependencies available to the agent
 *       - typically in the application scope or managed by the container</li>
 * </ul>
 * <p>
 * <b>Return types</b><br>
 * Action methods support two return patterns:
 * <ul>
 *   <li><strong>void</strong> - The action performs work with side effects (e.g., sending alerts,
 *       updating databases). No data is passed to subsequent phases.</li>
 *   <li><strong>Domain objects</strong> - The action returns an object (non-void) that will be
 *       automatically injected into subsequent life-cycle methods. Use this pattern
 *       to pass action results forward in the workflow.</li>
 * </ul>
 *
 * <p><b>Examples</b><br>
 * <pre>{@code
 * // Void return - performs side effects only
 * @Action
 * public void handleFraud(Fraud fraud, BankTransaction transaction) {
 *     if (fraud.isSerious()) {
 *         alertBankSecurity(fraud);
 *     }
 *     Customer customer = getCustomer(transaction);
 *     alertCustomer(fraud, transaction, customer);
 * }
 *
 * // Domain object return - passes result to outcome phase
 * @Action
 * public FraudReport processFraudCase(Fraud fraud, BankTransaction transaction) {
 *     FraudReport report = new FraudReport();
 *     report.setFraudType(fraud.getType());
 *     report.setTransaction(transaction);
 *     report.setTimestamp(System.currentTimeMillis());
 *     persistReport(report);
 *     return report;
 * }
 *
 * // Receives decision result object
 * @Action
 * public void executeDocumentation(DocumentationAnalysis analysis, PullRequest pr) {
 *     if (analysis.requiresDocumentation()) {
 *         generateDocs(analysis, pr);
 *         createDocumentationPullRequest(analysis);
 *     }
 * }
 *
 * // Later phases receive the action result
 * @Outcome
 * public void recordOutcome(FraudReport report, WorkflowContext context) {
 *     context.setAttribute("report", report);
 *     auditLog("Fraud case processed: " + report.getId());
 * }
 * }</pre>
 *
 * @see Decision
 * @see Outcome
 * @see WorkflowContext
 * @see LargeLanguageModel
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Action {
}
