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
 * Runtime exception thrown when a Large Language Model (LLM) operation fails.
 * <p>
 * This exception indicates errors during LLM processing such as:
 * <ul>
 *   <li>Communication failures with the LLM service</li>
 *   <li>Invalid responses from the LLM</li>
 *   <li>Rate limiting or quota exceeded errors</li>
 *   <li>Model unavailability or timeout</li>
 *   <li>Type conversion failures when processing LLM responses</li>
 * </ul>
 * <p>
 * As a runtime exception, it does not require explicit handling but can be 
 * caught by {@link HandleException} annotated methods in agent workflows.
 */
public class LLMException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new LLM exception with null as its detail message.
     */
    public LLMException() {
        super();
    }

    /**
     * Constructs a new LLM exception with the specified detail message.
     *
     * @param message The detail message.
     */
    public LLMException(String message) {
        super(message);
    }

    /**
     * Constructs a new LLM exception with the specified detail message and cause.
     *
     * @param message The detail message.
     * @param cause The cause (which is saved for later retrieval by the {@link #getCause()} method).
     */
    public LLMException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new LLM exception with the specified cause and a detail message 
     * of (cause==null ? null : cause.toString()).
     *
     * @param cause The cause (which is saved for later retrieval by the {@link #getCause()} method).
     */
    public LLMException(Throwable cause) {
        super(cause);
    }
}
