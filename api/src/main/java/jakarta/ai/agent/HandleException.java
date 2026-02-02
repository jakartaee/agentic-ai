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
 * Marks a method as an exception handler within an agent workflow.
 * <p>
 * Exception handler methods provide agent-specific error recovery when exceptions
 * occur during any workflow phase (trigger, decision, action, or outcome). The
 * handler is invoked with the thrown exception, allowing the agent to perform
 * recovery actions, logging, cleanup, or alternative workflows.
 * <p>
 * <b>Workflow Control:</b>
 * <ul>
 *   <li><strong>To continue workflow:</strong> Handler completes successfully (returns normally)</li>
 *   <li><strong>To stop workflow:</strong> Handler re-throws the exception or throws a new exception</li>
 * </ul>
 * <p>
 * <b>Parameters</b><br>
 * Exception handler methods can have the following types of parameters that will be
 * automatically resolved:
 * <ul>
 *   <li>Exception parameter (required) - The thrown exception or its supertype.
 *       The most specific matching handler is invoked based on exception type.</li>
 *   <li>Workflow state domain objects - Any objects from previous workflow phases,
 *       particularly the trigger event or decision/action results</li>
 *   <li>{@link WorkflowContext} - The current workflow context for state management
 *       and accessing stored attributes</li>
 *   <li>{@link LargeLanguageModel} - LLM instance for analysis or diagnostics</li>
 *   <li>Any other CDI injectable dependencies available to the agent
 *       - typically in the application scope or managed by the container</li>
 * </ul>
 * <p>
 * <b>Return type</b><br>
 * Exception handler methods MUST return {@code void}. Handlers are designed for
 * error recovery, logging, and cleanup rather than producing workflow data.
 *
 * <p><b>Semantics</b>
 * <ul>
 *   <li>Invoked when exceptions occur in any workflow phase</li>
 *   <li>Most specific exception type match is selected (follows Java exception hierarchy)</li>
 *   <li><strong>Workflow continues</strong> if handler returns normally (successful recovery)</li>
 *   <li><strong>Workflow stops</strong> if handler throws an exception (re-throw or new exception)</li>
 *   <li>If no matching handler exists, exception propagates to container</li>
 *   <li>Handler exceptions propagate to container (no recursive handling)</li>
 * </ul>
 *
 * <p><b>Examples</b><br>
 * <pre>{@code
 * // Recoverable error - returns normally, workflow continues
 * @HandleException
 * public void handleRecoverable(IOException ex, BankTransaction transaction) {
 *     logger.warn("I/O error, retrying transaction: " + transaction.getId(), ex);
 *     retryQueue.add(transaction);
 *     // Returns normally - workflow continues
 * }
 *
 * // Fatal error - re-throws, workflow stops
 * @HandleException
 * public void handleFatal(SecurityException ex, WorkflowContext context) {
 *     logger.error("Security violation", ex);
 *     auditService.logSecurityBreach(context.getTriggerEvent());
 *     throw ex; // Re-throw - workflow stops, propagates to container
 * }
 *
 * // Conditional recovery - decides whether to continue or stop
 * @HandleException
 * public void handleWithFallback(Exception ex, WorkflowContext context) {
 *     if (isRecoverable(ex)) {
 *         logger.info("Recovering from error", ex);
 *         performRecovery(context);
 *         // Returns normally - workflow continues
 *     } else {
 *         logger.error("Unrecoverable error", ex);
 *         throw new WorkflowFailureException("Unrecoverable error", ex);
 *         // Workflow stops
 *     }
 * }
 *
 * // Multiple handlers for different exception types
 * @HandleException
 * public void handleValidationError(ValidationException ex, WorkflowContext context) {
 *     logger.warn("Validation failed: " + ex.getMessage());
 *     context.setAttribute("validationFailed", true);
 *     // Returns normally - workflow continues with validation flag set
 * }
 *
 * @HandleException
 * public void handleGenericError(Exception ex, WorkflowContext context) {
 *     logger.error("Unexpected error", ex);
 *     alertService.notifyAdministrators(ex);
 *     throw ex; // Stop workflow for unexpected errors
 * }
 *
 * // Handler with LLM for diagnostics
 * @HandleException
 * public void handleWithDiagnostics(
 *     Exception ex,
 *     WorkflowContext context,
 *     LargeLanguageModel llm
 * ) {
 *     String analysis = llm.query(
 *         "Analyze this error and suggest recovery: " + ex.getMessage(),
 *         context.getTriggerEvent()
 *     );
 *     logger.info("LLM diagnostic: " + analysis);
 *     
 *     if (analysis.contains("recoverable")) {
 *         // Continue workflow based on LLM analysis
 *         return;
 *     }
 *     throw ex; // Stop if not recoverable
 * }
 * }</pre>
 *
 * @see Action
 * @see Decision
 * @see Outcome
 * @see WorkflowContext
 * @see LargeLanguageModel
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface HandleException {
}
