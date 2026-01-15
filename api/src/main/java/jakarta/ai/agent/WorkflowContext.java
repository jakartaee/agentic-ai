package jakarta.ai.agent;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the execution context for an agent workflow.
 * Stores intermediate results and state as the workflow progresses.
 *
 * <p>The workflow context acts as a shared data store that can be accessed
 * by different phases of the workflow (trigger, decision, action, outcome).
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
public class WorkflowContext {

    private final Map<String, Object> attributes = new HashMap<>();
    private Object triggerEvent;

    /**
     * Store a named attribute in the workflow context.
     *
     * @param name the attribute name
     * @param value the attribute value
     */
    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    /**
     * Retrieve a named attribute from the workflow context.
     *
     * @param name the attribute name
     * @return the attribute value, or null if not found
     */
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    /**
     * Get the triggering event that started this workflow.
     *
     * @return the trigger event object
     */
    public Object getTriggerEvent() {
        return triggerEvent;
    }

    /**
     * Set the triggering event.
     * This is typically called by the runtime when the workflow is initiated.
     *
     * @param event the trigger event
     */
    protected void setTriggerEvent(Object event) {
        this.triggerEvent = event;
    }
}
