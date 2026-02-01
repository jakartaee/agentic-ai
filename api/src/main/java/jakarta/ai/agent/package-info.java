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

/**
 * Jakarta Agentic AI - Core API for building autonomous agents.
 *
 * <p>This package provides the fundamental APIs for developing agents that can perceive,
 * reason, decide, and act autonomously within a Jakarta EE environment. Agents are
 * CDI-managed beans that encapsulate goal-driven behavior using a structured workflow
 * model with well-defined lifecycle phases.
 *
 * <h2>Core Components</h2>
 *
 * <h3>Agent Annotations</h3>
 * <ul>
 *   <li>{@link jakarta.ai.agent.Agent @Agent} - Marks a class as an agent</li>
 *   <li>{@link jakarta.ai.agent.Trigger @Trigger} - Marks a workflow trigger method</li>
 *   <li>{@link jakarta.ai.agent.Decision @Decision} - Marks a decision point in the workflow</li>
 *   <li>{@link jakarta.ai.agent.Action @Action} - Marks an action execution step</li>
 *   <li>{@link jakarta.ai.agent.Outcome @Outcome} - Marks the workflow outcome phase</li>
 *   <li>{@link jakarta.ai.agent.HandleException @HandleException} - Marks an exception handler</li>
 * </ul>
 *
 * <h3>Core Interfaces</h3>
 * <ul>
 *   <li>{@link jakarta.ai.agent.WorkflowContext} - Manages workflow state and context</li>
 *   <li>{@link jakarta.ai.agent.LargeLanguageModel} - Facade for LLM operations</li>
 * </ul>
 *
 * <h3>CDI Integration</h3>
 * <ul>
 *   <li>{@link jakarta.ai.agent.WorkflowScoped @WorkflowScoped} - Custom CDI scope for workflow lifecycle</li>
 * </ul>
 *
 * <h3>Data Models</h3>
 * <ul>
 *   <li>{@link jakarta.ai.agent.Result} - Standardized decision outcome record</li>
 * </ul>
 *
 * <h2>Workflow Execution Model</h2>
 *
 * <p>An agent workflow follows a structured lifecycle:
 *
 * <ol>
 *   <li><strong>Trigger Phase</strong> - {@link jakarta.ai.agent.Trigger @Trigger} methods are
 *       invoked when CDI events matching their parameters are fired. This initiates the workflow.</li>
 *   <li><strong>Decision Phase</strong> - {@link jakarta.ai.agent.Decision @Decision} methods
 *       analyze the workflow state and determine whether to proceed. They support three return patterns:
 *       <ul>
 *         <li>Boolean - {@code true} to proceed, {@code false} to terminate</li>
 *         <li>{@link jakarta.ai.agent.Result} - Success flag with optional details</li>
 *         <li>Object - Non-null to proceed (object available for injection), null to terminate</li>
 *       </ul>
 *   </li>
 *   <li><strong>Action Phase</strong> - {@link jakarta.ai.agent.Action @Action} methods
 *       execute the primary work based on decisions made in the decision phase.
 *       Actions support two return patterns:
 *       <ul>
 *         <li>Void - Performs side effects only (e.g., alerts, database updates)</li>
 *         <li>Domain Object - Returns an object available for injection in the subsequent workflow methods</li>
 *       </ul>
 *   </li>
 *   <li><strong>Outcome Phase</strong> - A single {@link jakarta.ai.agent.Outcome @Outcome} method
 *       handles finalization, state persistence, and downstream notifications. Must return void.</li>
 *   <li><strong>Exception Handling</strong> - {@link jakarta.ai.agent.HandleException @HandleException}
 *       methods handle exceptions throughout the workflow lifecycle.</li>
 * </ol>
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * import jakarta.ai.agent.*;
 * import jakarta.enterprise.inject.Produces;
 * import jakarta.inject.Inject;
 *
 * @Agent(name = "ExampleAgent", description = "An example autonomous agent")
 * public class ExampleAgent {
 *
 *     @Inject
 *     private LargeLanguageModel llm;
 *
 *     @Trigger
 *     public void onEvent(MyEvent event) {
 *         // Triggered by CDI event
 *     }
 *
 *     @Decision
 *     public boolean shouldProceed(MyEvent event) {
 *         String analysis = llm.query("Should we process this event?", event);
 *         return analysis.contains("yes");
 *     }
 *
 *     @Action
 *     public void executeAction(MyEvent event, WorkflowContext context) {
 *         // Perform the action with side effects
 *         notifySystem(event);
 *     }
 *
 *     @Outcome
 *     public void recordOutcome(WorkflowContext context) {
 *         // Finalize and persist results
 *     }
 * }
 * }</pre>
 *
 * <h2>CDI Integration</h2>
 *
 * <p>Agents are CDI-managed beans that integrate seamlessly with Jakarta EE:
 *
 * <ul>
 *   <li>Dependency injection of other CDI beans</li>
 *   <li>Custom {@link jakarta.ai.agent.WorkflowScoped @WorkflowScoped} scope for workflow-level lifecycle</li>
 *   <li>CDI event observation and production</li>
 *   <li>Interceptor support for cross-cutting concerns</li>
 * </ul>
 *
 * <h2>Large Language Model Integration</h2>
 *
 * <p>The {@link jakarta.ai.agent.LargeLanguageModel} interface provides a minimal,
 * type-converting facade for LLM operations. Implementations can support:
 *
 * <ul>
 *   <li>Text prompts with optional input objects</li>
 *   <li>String and domain object returns</li>
 *   <li>Unwrapping for vendor-specific features</li>
 * </ul>
 *
 * <h2>Workflow Context Management</h2>
 *
 * <p>The {@link jakarta.ai.agent.WorkflowContext} interface manages state throughout
 * the workflow lifecycle:
 *
 * <ul>
 *   <li>Attribute storage and retrieval - Store workflow state as phases progress</li>
 *   <li>Automatic trigger event preservation - Accessible via {@code getTriggerEvent()}</li>
 *   <li>Workflow-scoped lifecycle - Destroyed when workflow completes</li>
 * </ul>
 *
 * <h2>Parameter Injection</h2>
 *
 * <p>Workflow methods automatically receive parameters based on type resolution:
 *
 * <ul>
 *   <li>Trigger event objects - From the workflow trigger</li>
 *   <li>Decision results - Objects returned from {@link jakarta.ai.agent.Decision @Decision} methods</li>
 *   <li>Action results - Objects returned from {@link jakarta.ai.agent.Action @Action} methods</li>
 *   <li>{@link jakarta.ai.agent.WorkflowContext} - Current workflow context</li>
 *   <li>{@link jakarta.ai.agent.LargeLanguageModel} - Injected LLM facade</li>
 *   <li>Any CDI injectable dependencies - Standard Jakarta EE beans and resources</li>
 * </ul>
 *
 * @see jakarta.ai.agent.Agent
 * @see jakarta.ai.agent.Trigger
 * @see jakarta.ai.agent.Decision
 * @see jakarta.ai.agent.Action
 * @see jakarta.ai.agent.Outcome
 * @see jakarta.ai.agent.WorkflowContext
 * @see jakarta.ai.agent.LargeLanguageModel
 * @see jakarta.ai.agent.WorkflowScoped
 */
package jakarta.ai.agent;
