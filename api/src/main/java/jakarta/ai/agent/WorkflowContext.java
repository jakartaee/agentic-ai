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

import java.util.Set;

/**
 * Represents the execution context for an agent workflow.
 * Stores intermediate results and state as the workflow progresses.
 * <p>
 * The workflow context acts as a shared data store that can be accessed
 * by different phases of the workflow (trigger, decision, action, outcome).
 * The context is created when a workflow is triggered and exists for the
 * duration of the workflow execution.
 * <p>
 * Implementations of this interface are provided by the Jakarta Agentic AI
 * runtime and are scoped to a single workflow execution.
 * <p>
 * The workflow context can be included as a parameter in annotated agent
 * life-cycle methods, or injected via @Inject into the agent class.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Agent
 * public class MyAgent {
 *
 *     @Decision
 *     public boolean analyze(WorkflowContext context, Task task) {
 *         // Store intermediate results
 *         context.setAttribute("analysisResult", performAnalysis(task));
 *         return true;
 *     }
 *
 *     @Action
 *     public void execute(WorkflowContext context) {
 *         // Retrieve previous results
 *         Object result = context.getAttribute("analysisResult");
 *         processResult(result);
 *     }
 * }
 * }</pre>
 */
public interface WorkflowContext {

    /**
     * Store a named attribute in the workflow context.
     * <p>
     * If an attribute with the same name already exists, it is replaced.
     * If the value is {@code null}, the effect is the same as calling
     * {@link #removeAttribute(String)}.
     *
     * @param name the attribute name, must not be {@code null}
     * @param value the attribute value
     * @throws IllegalArgumentException if name is {@code null}
     */
    void setAttribute(String name, Object value);

    /**
     * Retrieve a named attribute from the workflow context.
     *
     * @param name the attribute name
     * @return the attribute value, or {@code null} if not found
     */
    Object getAttribute(String name);

    /**
     * Remove a named attribute from the workflow context.
     * <p>
     * If the attribute does not exist, this method does nothing.
     *
     * @param name the attribute name
     */
    void removeAttribute(String name);

    /**
     * Get all attribute names in the workflow context.
     *
     * @return a set of attribute names, never {@code null}
     */
    Set<String> getAttributeNames();

    /**
     * Get the triggering event that started this workflow.
     * <p>
     * The trigger event is set by the runtime when the workflow is initiated
     * and represents the event or data that caused the workflow to begin.
     *
     * @return the trigger event object, or {@code null} if not set
     */
    Object getTriggerEvent();
}
