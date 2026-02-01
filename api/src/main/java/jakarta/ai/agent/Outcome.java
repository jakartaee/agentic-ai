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
 * Marks a method as the outcome phase of an agent workflow.
 * <p>
 * Outcome methods denote the completion of the agent's workflow and produce
 * final results, side effects, or state management. The outcome phase executes
 * after action methods have completed successfully, providing an opportunity
 * to finalize workflow results, audit operations, trigger downstream processes,
 * or clean up resources.
 * <p>
 * There can currently be only one {@code @Outcome} method per agent class.
 * This will likely be relaxed in future versions to adapt to more complex 
 * dynamic workflows.
 * <p>
 * <b>Parameters</b><br>
 * Outcome methods can have the following types of parameters that will be
 * automatically resolved:
 * <ul>
 *   <li>Workflow state domain objects - Any objects used by prior phases in the 
 *       workflow - triggers, decisions, and actions</li>
 *   <li>{@link WorkflowContext} - The current workflow context for state management
 *       and accessing stored attributes</li>
 *   <li>{@link LargeLanguageModel} - LLM instance for analysis, summarization,
 *       or content generation based on workflow results</li>
 *   <li>Any other CDI injectable dependencies available to the agent
 *       - typically in the application scope or managed by the container</li>
 * </ul>
 * <p>
 * <b>Return type</b><br>
 * Outcome methods currently must return {@code void}. The outcome phase is designed
 * for finalization and side effects rather than producing data for further processing.
 * In the future, return types may be supported to allow outcome methods to pass on 
 * results to downstream systems or workflows.
 *
 * <p><b>Semantics</b>
 * <ul>
 *   <li>Outcome methods execute after all {@link Action @Action} methods complete</li>
 *   <li>Marks the end of successful workflow execution</li>
 *   <li>The current workflow context will be destroyed by the container
 *       after outcome completion</li>
 * </ul>
 *
 * <p><b>Examples</b><br>
 * <pre>{@code
 * // Simple outcome - finalizes workflow state
 * @Outcome
 * public void finalizeTransaction(BankTransaction transaction) {
 *     transaction.setProcessed(true);
 *     transaction.setProcessedTime(System.currentTimeMillis());
 *     entityManager.merge(transaction);
 * }
 *
 * // Outcome receiving action result
 * @Outcome
 * public void recordOutcome(FraudReport report, WorkflowContext context) {
 *     auditLog("Fraud case processed: " + report.getId());
 * }
 *
 * // Outcome with multiple decision/action results
 * @Outcome
 * public void publishResults(
 *     DocumentationFiles generatedFiles,
 *     PullRequestEvent originalEvent,
 *     WorkflowContext context
 * ) {
 *     String docPrUrl = (String) context.getAttribute("docPrUrl");
 *     notificationService.notifyUser(
 *         originalEvent.getAuthor(),
 *         "Documentation generated at: " + docPrUrl
 *     );
 * }
 *
 * // Outcome with LLM for summarization
 * @Outcome
 * public void summarizeAndArchive(
 *     CustomerSupportResponse response,
 *     WorkflowContext context,
 *     LargeLanguageModel llm
 * ) {
 *     String summary = llm.query(
 *         "Summarize the customer support interaction",
 *         response
 *     );
 *     ticketService.archive(context.getTriggerEvent(), summary);
 * }
 * }</pre>
 *
 * @see Action
 * @see Decision
 * @see Trigger
 * @see WorkflowContext
 * @see LargeLanguageModel
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Outcome {
}
