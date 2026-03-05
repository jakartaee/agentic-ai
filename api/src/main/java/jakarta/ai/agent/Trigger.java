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
 * when triggering events occur. Currently, triggers are invoked by CDI events.
 * A trigger method may optionally use the {@code @Observes} annotation to 
 * explicitly handle the triggering event. The triggering event is automatically 
 * added to the workflow context for access in subsequent phases.
 * <p>
 * <b>Scope Restrictions</b><br>
 * CDI event observation capabilities depend on the agent's scope:
 * <ul>
 *   <li><strong>@WorkflowScoped agents:</strong> Can ONLY use {@code @Trigger} 
 *       methods to handle CDI events. General CDI observers (methods with 
 *       {@code @Observes} but without {@code @Trigger}) are not supported.</li>
 *   <li><strong>@ApplicationScoped agents:</strong> Can use both 
 *       {@code @Trigger} methods AND general CDI observer methods.</li>
 * </ul>
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
 *   <li>The triggering event (CDI event) - Optionally annotated with
 *       {@code @Observes} to explicitly declare CDI event observation</li>
 *   <li>{@link LargeLanguageModel} - LLM instance for trigger analysis</li>
 *   <li>Any other CDI injectable dependencies available to the agent</li>
 * </ul>
 * <p>
 * Parameters can declare Jakarta Validation constraints (e.g., {@code @Valid},
 * {@code @NotNull}, {@code @Email}, {@code @Size}). Validation occurs before
 * the trigger method is invoked. Validation failures raise 
 * {@code jakarta.validation.ConstraintViolationException}, which can be
 * handled by {@link HandleException @HandleException} methods.
 * <p>
 * <b>Return types</b><br>
 * Trigger methods support two return patterns:
 * <ul>
 *   <li><strong>void</strong> - The trigger handles initialization with 
 *       side effects only. No data is passed to subsequent phases.</li>
 *   <li><strong>Domain objects</strong> - The trigger returns an
 *       object (non-void) that will be automatically injected into
 *       subsequent workflow methods. Use this pattern to pass trigger 
 *       analysis or transformation forward in the workflow.</li>
 * </ul>
 * <p>
 * <b>Semantics</b>
 * <ul>
 *   <li>Invoked when a CDI event matching the trigger parameter is fired</li>
 *   <li>First phase of workflow execution - creates a new workflow 
 *       context</li>
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
 * }</pre>
 *
 * @see Decision
 * @see Action
 * @see Outcome
 * @see LargeLanguageModel
 *
 * @since 1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Trigger {
}
