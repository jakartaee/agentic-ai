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
 * Trigger methods define the entry point for agent workflows and are invoked
 * when triggering events occur. Currently, triggers are invoked by CDI events,
 * where the runtime automatically observes CDI events matching the trigger
 * method's parameter type. The triggering event is automatically added to
 * the workflow context for access in subsequent phases.
 * <p>
 * While triggers are currently limited to CDI events, future versions may 
 * support other event sources, such as Jakarta Messaging messages, 
 * manual/programmatic workflow invocation, or REST POST requests.
 * <p>
 * Currently, there MUST be only one {@code @Trigger} method per agent class.
 * This constraint will be relaxed in future versions to support multiple 
 * entry points.
 * <p>
 * <b>Parameters</b><br>
 * Trigger methods can have the following types of parameters:
 * <ul>
 *   <li>The triggering event (CDI event) - Automatically detected and matched by the runtime</li>
 *   <li>{@link WorkflowContext} - The current workflow context for initialization</li>
 *   <li>{@link LargeLanguageModel} - LLM instance for trigger analysis</li>
 *   <li>Any other CDI injectable dependencies available to the agent</li>
 * </ul>
 * <p>
 * <b>Return types</b><br>
 * Trigger methods support two return patterns:
 * <ul>
 *   <li><strong>void</strong> - The trigger handles initialization with side effects only.
 *       No data is passed to subsequent phases.</li>
 *   <li><strong>Domain objects</strong> - The trigger returns an object (non-void) that will be
 *       automatically injected into subsequent workflow methods. Use this pattern
 *       to pass trigger analysis or transformation forward in the workflow.</li>
 * </ul>
 * <p>
 * <b>Semantics</b>
 * <ul>
 *   <li>Invoked when a CDI event matching the trigger parameter is fired</li>
 *   <li>First phase of workflow execution - creates a new workflow context</li>
 *   <li>The triggering event is automatically stored in {@link WorkflowContext}</li>
 * </ul>
 * <p>
 * <b>Examples</b><br>
 * <pre>{@code
 * // Void return - initialization with side effects
 * @Trigger
 * public void onTransaction(BankTransaction event) {
 *     logger.info("Workflow triggered for transaction: " + event.getId());
 *     // Initialization logic only
 * }
 *
 * // Void return with context
 * @Trigger
 * public void onEvent(MyEvent event, WorkflowContext context) {
 *     context.setAttribute("eventTime", System.currentTimeMillis());
 *     context.setAttribute("eventType", event.getType());
 * }
 *
 * // Domain object return - passes analysis to next phases
 * @Trigger
 * public EventAnalysis analyzeEvent(MyEvent event) {
 *     EventAnalysis analysis = new EventAnalysis();
 *     analysis.setSource(event.getSource());
 *     analysis.setPriority(determinePriority(event));
 *     return analysis;
 * }
 *
 * // Trigger with LLM analysis
 * @Trigger
 * public TriggerResult analyzeTrigger(MyEvent event, LargeLanguageModel llm) {
 *     String analysis = llm.query("Classify this event: " + event.toString());
 *     TriggerResult result = new TriggerResult();
 *     result.setClassification(analysis);
 *     result.setEventData(event);
 *     return result;
 * }
 *
 * // Trigger with full context initialization
 * @Trigger
 * public InitialAnalysis initializeWorkflow(
 *     BankTransaction event,
 *     WorkflowContext context,
 *     LargeLanguageModel llm
 * ) {
 *     InitialAnalysis analysis = new InitialAnalysis();
 *     analysis.setTransaction(event);
 *     
 *     // Store in context for later phases
 *     context.setAttribute("transaction", event);
 *     context.setAttribute("timestamp", System.currentTimeMillis());
 *     
 *     // Perform initial LLM analysis
 *     String risk = llm.query("Assess transaction risk: " + event.getAmount());
 *     analysis.setRiskLevel(parseRiskLevel(risk));
 *     
 *     return analysis;
 * }
 * }</pre>
 *
 * @see Decision
 * @see Action
 * @see Outcome
 * @see WorkflowContext
 * @see LargeLanguageModel
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Trigger {
}
