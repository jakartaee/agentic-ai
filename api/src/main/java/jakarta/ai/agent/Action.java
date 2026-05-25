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
 * Actions can execute independently or based on prior decision outcomes,
 * allowing both simple sequential processing and complex conditional 
 * workflows. Actions typically perform the primary work of the agent, such 
 * as persisting data, calling external services, or updating system state.
 * <p>
 * Multiple action methods can be defined in a single agent. Actions and
 * decisions can be intermixed to create sophisticated branching logic.
 * <p>
 * <b>Execution Order</b><br>
 * The execution order of {@code @Action} and {@code @Decision} methods is
 * determined by the following precedence rules, applied in order:
 * <ol>
 *   <li><strong>{@link jakarta.annotation.Priority @Priority} on the
 *       method</strong> — the annotation value is the sort key; lower
 *       values execute first.</li>
 *   <li><strong>{@link #order()} attribute</strong> — used as the sort key
 *       when {@code @Priority} is absent on the method; lower values
 *       execute first.</li>
 *   <li><strong>Source declaration order</strong> — used when no action 
 *       or decision method in the agent declares either {@code @Priority} 
 *       or an explicit {@code order}. In this case, methods execute in 
 *       the order they are declared in the source code. Note that
 *       Java SE does not guarantee that reflection returns methods in source
 *       declaration order. However, all major JVM implementations do so in
 *       practice. Portable applications that require a strict, guaranteed
 *       execution order must use {@code @Priority} or {@code order}.</li>
 * </ol>
 * <strong>Consistency requirement</strong>: if any {@code @Action} or
 * {@code @Decision} method in an agent declares an explicit {@code order}
 * or {@code @Priority}, every {@code @Action} and {@code @Decision} method
 * in that agent must do the same. Mixing explicitly ordered and unordered
 * methods is a deployment error.
 * <p>
 * <b>Parameters</b><br>
 * Action methods can have the following types of parameters that will be
 * automatically resolved:
 * <ul>
 *   <li>Workflow state domain objects - Any objects from previous 
 *       workflow phases, particularly decision results or trigger event 
 *       objects</li>
 *   <li>Decision results - Objects returned from 
 *       {@link Decision @Decision} methods (when using the Object or 
 *       {@link Result} return patterns)</li>
 *   <li>{@link LargeLanguageModel} - LLM instance for analysis or 
 *       content generation</li>
 *   <li>Any other CDI injectable dependencies available to the agent
 *       - typically in the application scope or managed by the 
 *       container</li>
 * </ul>
 * <p>
 * Parameters can declare Jakarta Validation constraints (e.g., {@code @Valid},
 * {@code @NotNull}, {@code @NotEmpty}). Validation occurs before the action 
 * method is invoked. Validation failures raise 
 * {@code jakarta.validation.ConstraintViolationException}, which can be
 * handled by {@link HandleException @HandleException} methods.
 * <p>
 * <b>Return types</b><br>
 * Action methods support two return patterns:
 * <ul>
 *   <li><strong>void</strong> - The action performs work with side 
 *       effects (e.g., sending alerts, updating databases). No data is 
 *       passed to subsequent phases.</li>
 *   <li><strong>Domain objects</strong> - The action returns an 
 *       object (non-void) that will be automatically injected into 
 *       subsequent life-cycle methods. Use this pattern
 *       to pass action results forward in the workflow.</li>
 * </ul>
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
 * public void recordOutcome(FraudReport report) {
 *     auditLog("Fraud case processed: " + report.getId());
 * }
 * }</pre>
 *
 * @see Decision
 * @see Outcome
 * @see LargeLanguageModel
 *
 * @since 1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Action {

    /**
     * The position of this action in the workflow execution sequence.
     * Lower values execute first.
     * <p>
     * Ignored when {@link jakarta.annotation.Priority @Priority} is also
     * present on the method — {@code @Priority} takes precedence.
     * <p>
     * Defaults to {@code 0}. When all {@code @Action} and {@code @Decision}
     * methods in an agent use the default value, source declaration order
     * determines execution order. If any method in the agent explicitly sets
     * {@code order} or declares {@code @Priority}, all other {@code @Action}
     * and {@code @Decision} methods in that agent must also declare one.
     *
     * @return the sort key for this action; lower values execute first
     */
    int order() default 0;
}
