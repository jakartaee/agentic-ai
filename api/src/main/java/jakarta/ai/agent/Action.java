package jakarta.ai.agent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as an action step in the agent workflow.
 * <p>
 * An action method performs a single step or operation in the agent's workflow.
 * Actions are typically executed sequentially as defined by the workflow.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Action {}
