package jakarta.ai.agent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as a decision point in the agent workflow.
 * <p>
 * A decision method evaluates workflow state and determines the next step or outcome.
 * The return value controls workflow branching or completion.
 * <p>
 * You can return a boolean or the standard {@link Result} record to indicate
 * success/failure and provide additional details for workflow handling.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Decision {}
