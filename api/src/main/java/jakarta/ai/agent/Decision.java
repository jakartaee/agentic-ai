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
 * and can be intermixed with actions to create conditional branching logic.
 * <p>
 * <b>Execution Order</b><br>
 * The execution order of {@code @Decision} and {@code @Action} methods is
 * determined by the following precedence rules, applied in order:
 * <ol>
 *   <li><strong>{@link jakarta.annotation.Priority @Priority} on the
 *       method</strong> — the annotation value is the sort key; lower
 *       values execute first.</li>
 *   <li><strong>{@link #order()} attribute</strong> — used as the sort key
 *       when {@code @Priority} is absent on the method; lower values
 *       execute first.</li>
 *   <li><strong>Source declaration order</strong> — used when no action
 *       or decision method in the agent declares either {@code @Priority}
 *       or an explicit {@code order}. In this case, methods execute in
 *       the order they are declared in the source code. Note that
 *       Java SE does not guarantee that reflection returns methods in source
 *       declaration order. However, all major JVM implementations do so in
 *       practice. Portable applications that require a strict, guaranteed
 *       execution order must use {@code @Priority} or {@code order}.</li>
 * </ol>
 * <strong>Consistency requirement</strong>: if any {@code @Decision} or
 * {@code @Action} method in an agent declares an explicit {@code order}
 * or {@code @Priority}, every {@code @Decision} and {@code @Action} method
 * in that agent must do the same. Mixing explicitly ordered and unordered
 * methods is a deployment error.
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

    /**
     * The position of this decision in the workflow execution sequence.
     * Lower values execute first.
     * <p>
     * Ignored when {@link jakarta.annotation.Priority @Priority} is also
     * present on the method — {@code @Priority} takes precedence.
     * <p>
     * Defaults to {@code 0}. When all {@code @Decision} and {@code @Action}
     * methods in an agent use the default value, source declaration order
     * determines execution order. If any method in the agent explicitly sets
     * {@code order} or declares {@code @Priority}, all other {@code @Decision}
     * and {@code @Action} methods in that agent must also declare one.
     *
     * @return the sort key for this decision; lower values execute first
     */
    int order() default 0;
}
