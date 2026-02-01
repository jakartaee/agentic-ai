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
 * Declares a class as an AI agent.
 * <p>
 * An agent is a CDI bean that encapsulates autonomous, goal-driven behavior.
 * The agent's lifecycle, workflow, and actions are fully defined using 
 * additional annotations.
 * <p>
 * Agents support two scope annotations: {@link WorkflowScoped} and @ApplicationScoped.
 * If no scope annotation is present on the class, the agent will be 
 * assumed to be {@link WorkflowScoped}. 
 * <p>
 * A workflow context will still be created for each workflow execution even when the 
 * agent is @ApplicationScoped, beginning with a trigger and in most cases 
 * ending with an outcome.
 * </p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Agent {

    /**
     * The agent's name.
     * <p>
     * If not specified, the agent's class name in camelCase will be used as a default.
     */
    String name() default "";

    /**
     * The agent's description.
     * <p>
     * Used for documentation and discovery purposes.
     */
    String description() default "";
}
