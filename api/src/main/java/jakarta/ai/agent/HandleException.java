package jakarta.ai.agent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as an exception handler within an agent workflow.
 * <p>
 * When an exception occurs during workflow execution (in trigger, decision,
 * action, or outcome phases), methods annotated with {@code @HandleException}
 * will be invoked to handle the error gracefully.
 * <p>
 * Exception handler methods can accept the thrown exception as a parameter
 * and may also receive the {@link WorkflowContext} to access workflow state.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Agent
 * public class FraudDetectionAgent {
 *
 *     @Decision
 *     public boolean checkFraud(BankTransaction transaction) {
 *         // May throw exception
 *         return analyzeFraud(transaction);
 *     }
 *
 *     @HandleException
 *     public void handleError(Exception ex, WorkflowContext context) {
 *         logger.error("Workflow failed: " + ex.getMessage());
 *         // Implement recovery logic or logging
 *     }
 * }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface HandleException {
}
