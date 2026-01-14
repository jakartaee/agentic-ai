package jakarta.ai.agent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as an external trigger that initiates an agent workflow.
 * <p>
 * A trigger method initiates the agent's workflow, typically in response to a CDI event
 * or other external stimulus. The method may accept parameters that are added to the
 * workflow context.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Agent
 * public class FraudDetectionAgent {
 *
 *     @Trigger
 *     public void processTransaction(BankTransaction transaction) {
 *         // Workflow trigger logic - initiates the agent workflow
 *     }
 * }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Trigger {
}
