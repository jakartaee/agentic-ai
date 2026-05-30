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

/**
 * Minimal facade for Large Language Model (LLM) operations.
 * <p>
 * Intended to be injected via CDI into agents. Provides a unified interface
 * for parameterized querying of LLMs with support for type conversion of
 * parameters and results.
 * <p>
 * Implementations must use Jakarta JSON Binding for serialization of
 * structured prompt parameters and deserialization of typed responses. This
 * ensures consistent, portable behavior across implementations.
 * <p>
 * For the varargs query methods, supplied parameters are positional. The exact
 * token {@code {}} in the prompt acts as a placeholder marker, similar to
 * well-known logger APIs. Implementations must substitute placeholders in
 * declaration order using the Jakarta JSON Binding serialization of the
 * corresponding parameter. When the prompt contains one or more
 * {@code {}} placeholders, the number of supplied parameters must exactly
 * match the number of placeholders. When the prompt contains no placeholder,
 * at most one supplied parameter may be made available to the model as
 * structured context. Only the exact token {@code {}} is treated as a
 * placeholder; other brace usage remains literal prompt text.
 * A single supplied parameter is therefore valid either as the value for one
 * {@code {}} placeholder or as structured context when the prompt contains no
 * placeholder.
 * <pre>{@code
 * llm.query("Classify this event: {}", event);
 * llm.query("Classify this event", event);
 * }</pre>
 * <p>
 * Implementations must maintain conversational state for the current workflow
 * context across query calls. For {@link WorkflowScoped} agents, that
 * conversational state is bound to the workflow context and must end when the
 * workflow context ends. For {@code @ApplicationScoped} agents,
 * conversational state must remain isolated per workflow context and must
 * not leak across concurrent invocations.
 * <p>
 * Implementations must be thread-safe within a single workflow context.
 * <p>
 * Implementations will delegate to external LLM APIs or services.
 * <p>
 * In the initial release, implementations are free to support whichever LLM
 * libraries and APIs they choose. Configuration mechanisms (if any) are 
 * implementation-specific. Future releases will provide standardized provider 
 * selection and some common LLM configuration, allowing developers to switch 
 * between different LLM implementations, and rely on very common configurable
 * LLM features in a standardized way. Common examples may include temperature 
 * or maximum output tokens. This is very similar to how Jakarta 
 * Persistence works with multiple providers and a common set of configuration 
 * properties.
 *
 * @see LLMException
 *
 * @since 1.0
 */
public interface LargeLanguageModel {

    /**
     * Sends a prompt to the model and returns a String response.
     * <p>
     * This is the simplest form of LLM interaction, suitable for plain text 
     * prompts and responses.
     *
     * @param prompt The input prompt or question.
     * @return The model's response as a String.
     * @throws IllegalArgumentException if the prompt is null or invalid.
     * @throws LLMException if the LLM service encounters an error 
     *                      during processing.
     */
    String query(String prompt);

    /**
     * Sends a prompt to the model and returns a response of the specified
     * type.
     * <p>
     * The LLM response (expected to be JSON) is deserialized to the requested
     * type using Jakarta JSON Binding.
     *
     * @param prompt The prompt or query.
     * @param resultType The expected result type.
     * @param <T> The type of the result.
     * @return The model's response converted to the specified type.
     * @throws IllegalArgumentException if the prompt or resultType is null.
     * @throws LLMException if the LLM service encounters an error during
     *                      processing, or if the response cannot be
     *                      deserialized to the requested type.
     */
    <T> T query(String prompt, Class<T> resultType);

    /**
     * Sends a prompt template and a variable number of parameters to the model,
     * returning a String response.
     * <p>
     * Parameters are serialized to JSON using Jakarta JSON Binding. The exact
     * token {@code {}} in the prompt indicates a positional substitution point.
     * Implementations must substitute parameters in declaration order, similar
     * to well-known logger APIs. When the prompt contains one or more
     * {@code {}} placeholders, the number of supplied parameters must exactly
     * match the number of placeholders. When the prompt contains no
     * placeholder, at most one supplied parameter may still be sent to the LLM
     * as structured context.
     *
     * @param prompt The prompt or prompt template.
     * @param parameters The positional parameters, or a single structured
     *                   context object when the prompt contains no placeholder.
     * @return The model's response as a String.
     * @throws IllegalArgumentException if the prompt is null, if the number of
     *                                  supplied parameters does not match the
     *                                  number of {@code {}} placeholders,
     *                                  except that a prompt with no placeholder
     *                                  may accept at most one supplied
     *                                  parameter, or if a parameter cannot be
     *                                  serialized to JSON.
     * @throws LLMException if the LLM service encounters an error during 
     *                      processing.
     */
    String query(String prompt, Object... parameters);

    /**
     * Sends a prompt template and a variable number of parameters to the model,
     * returning a response of the specified type.
     * <p>
     * Parameters are serialized to JSON using Jakarta JSON Binding. The exact
     * token {@code {}} in the prompt indicates a positional substitution point.
     * Implementations must substitute parameters in declaration order, similar
     * to well-known logger APIs. When the prompt contains one or more
     * {@code {}} placeholders, the number of supplied parameters must exactly
     * match the number of placeholders. When the prompt contains no
     * placeholder, at most one supplied parameter may still be sent to the LLM
     * as structured context. The LLM response (expected to be JSON) is
     * deserialized to the requested type using Jakarta JSON Binding.
     *
     * @param prompt The prompt or prompt template.
     * @param resultType The expected result type.
     * @param parameters The positional parameters, or a single structured
     *                   context object when the prompt contains no placeholder.
     * @param <T> The type of the result.
     * @return The model's response converted to the specified type.
     * @throws IllegalArgumentException if the prompt or resultType is null,
     *                                  if the number of supplied parameters
     *                                  does not match the number of
     *                                  {@code {}} placeholders, except that a
     *                                  prompt with no placeholder may accept at
     *                                  most one supplied parameter, or if a
     *                                  parameter cannot be converted.
     * @throws LLMException if the LLM service encounters an error during
     *                      processing, or if the response cannot be
     *                      deserialized to the requested type.
     */
    <T> T query(String prompt, Class<T> resultType, Object... parameters);

    /**
     * Unwraps the underlying LLM implementation.
     * <p>
     * This allows access to vendor-specific APIs or advanced features not 
     * exposed by the facade. Similar to Jakarta Persistence's 
     * {@code EntityManager.unwrap()} pattern.
     *
     * @param implClass The class of the underlying implementation to unwrap to.
     * @param <T> The type of the underlying implementation.
     * @return The underlying implementation instance.
     * @throws IllegalArgumentException if the implementation cannot be 
     *                                  unwrapped to the requested type.
     */
    <T> T unwrap(Class<T> implClass);
}
