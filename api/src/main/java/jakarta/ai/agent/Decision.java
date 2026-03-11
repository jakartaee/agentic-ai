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
 * Marks a method as a decision point in an agent workflow.
 * <p>
 * Decision methods are optional workflow phases that determine whether and
 * how the workflow should proceed. Multiple decision methods can be defined 
 * and will execute in declaration order. Decisions can be intermixed with 
 * actions to create conditional branching logic.
 * <p>
 * Decision methods typically use a {@link LargeLanguageModel} to analyze the
 * workflow state and make intelligent decisions.
 * <p>
 * <b>Parameters</b><br>
 * Decision methods can have the following types of parameters that will be
 * automatically resolved:
 * <ul>
 *   <li>Workflow state domain objects - Any objects used by prior phases 
 *       in the workflow, particularly the triggering event object</li>
 *   <li>{@link LargeLanguageModel} - LLM instance</li>
 *   <li>Any other CDI injectable dependencies available to the agent
 *       - typically in the application scope or managed by the 
 *       container</li>
 * </ul>
 * <p>
 * Parameters can declare Jakarta Validation constraints (e.g., {@code @Valid},
 * {@code @NotNull}, {@code @Email}). Validation occurs before the decision 
 * method is invoked. Validation failures raise 
 * {@code jakarta.validation.ConstraintViolationException}, which can be
 * handled by {@link HandleException @HandleException} methods.
 * <p>
 * <b>Return types</b><br>
 * Decision methods support multiple return patterns:
 * <ul>
 *   <li><strong>Boolean</strong>: {@code true} means proceed with the 
 *       workflow, {@code false} means stop the workflow</li>
 *   <li><strong>{@link Result}</strong>: A {@code Result} record with 
 *       success flag and optional details to control workflow and pass 
 *       data to subsequent phases</li>
 *   <li><strong>Object</strong>: A non-null object means proceed (and 
 *       the object is available for injection into subsequent phases), 
 *       {@code null} means stop the workflow</li>
 * </ul>
 * <p>
 * <b>Examples</b><br>
 * <pre>{@code
 * // Boolean return
 * @Decision
 * public boolean shouldGenerateDocs(PullRequest pr) {
 *     String response = llm.query(
 *         "Does this PR require documentation updates?",
 *         pr);
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
 *         "Analyze what documentation is needed for this pull request",
 *         pr);
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
 * @see Trigger
 * @see Action
 * @see Outcome
 *
 * @since 1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Decision {
}
