package jakarta.ai.agent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as a trigger for starting an agent workflow.
 * <p>
 * A trigger method initiates the agent's workflow, typically in response to a CDI event or other external stimulus.
 * The method may accept parameters that are added to the workflow context.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Trigger {}
