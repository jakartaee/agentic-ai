package jakarta.ai.agent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as the outcome (end) of an agent workflow.
 * <p>
 * An outcome method denotes the completion of the agent's workflow and may produce a final result or side effect.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Outcome {}
