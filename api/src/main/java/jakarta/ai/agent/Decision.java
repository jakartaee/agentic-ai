package jakarta.ai.agent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as a decision point in an agent workflow.
 *
 * Decision methods are invoked after the @Trigger phase and determine whether
 * the workflow should proceed to the @Action phase. They typically use a
 * {@link LargeLanguageModel} to analyze the trigger event and make intelligent
 * decisions.
 *
 * <h2>Return Types</h2>
 * Decision methods support multiple return patterns:
 * <ul>
 *   <li><strong>Boolean</strong>: {@code true} means proceed to actions,
 *       {@code false} means stop the workflow</li>
 *   <li><strong>{@link Result}</strong>: A {@code Result} record with success flag
 *       and optional details to control workflow and pass data to subsequent phases</li>
 *   <li><strong>Object</strong>: A non-null object means proceed (and the object
 *       is available for injection into subsequent phases), {@code null} means
 *       stop the workflow</li>
 * </ul>
 *
 * <h2>Examples</h2>
 *
 * <pre>{@code
 * // Boolean return
 * @Decision
 * public boolean shouldGenerateDocs(PullRequest pr) {
 *     String response = llm.query(
 *         "Does this PR require documentation updates? " + pr.getDiff(), pr);
 *     return response.contains("yes");
 * }
 *
 * // Result return - provides structured outcome
 * @Decision
 * public Result checkFraud(BankTransaction transaction) {
 *     String output = llm.query(
 *         "Is this a fraudulent transaction?", transaction);
 *     boolean fraud = isFraud(output);
 *     Fraud details = fraud ? getFraudDetails(output) : null;
 *     return new Result(fraud, details);
 * }
 *
 * // Object return - provides data for next phases
 * @Decision
 * public DocumentationPlan planDocumentation(PullRequest pr) {
 *     String analysis = llm.query(
 *         "Analyze what documentation is needed for: " + pr.getDiff(), pr);
 *
 *     if (analysis.contains("no documentation needed")) {
 *         return null;  // Stop workflow
 *     }
 *
 *     return new DocumentationPlan(analysis);  // Proceed with this plan
 * }
 *
 * // Later phases can receive the decision result
 * @Action
 * public void generateDocs(DocumentationPlan plan) {
 *     // Use the plan from the decision phase
 *     createDocumentation(plan.getFiles(), plan.getContent());
 * }
 * }</pre>
 *
 * <h2>Parameter Injection</h2>
 * Decision methods can receive parameters that will be automatically resolved:
 * <ul>
 *   <li>{@link WorkflowContext} - The current workflow context</li>
 *   <li>Trigger event object - The event that started the workflow</li>
 *   <li>{@link LargeLanguageModel} - Injected LLM instance (via CDI)</li>
 * </ul>
 *
 * @see Trigger
 * @see Action
 * @see Outcome
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Decision {
}
