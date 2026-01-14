package jakarta.ai.agent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as the outcome (end) of an agent workflow.
 * <p>
 * An outcome method denotes the completion of the agent's workflow and may produce
 * a final result or side effect. This phase executes after actions have completed.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Agent
 * public class FraudDetectionAgent {
 *
 *     @Outcome
 *     public void finalizeTransaction(BankTransaction transaction) {
 *         // Mark transaction as processed
 *         transaction.setProcessed(true);
 *         entityManager.merge(transaction);
 *     }
 * }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Outcome {
}
