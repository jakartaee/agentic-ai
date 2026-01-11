package jakarta.ai.agent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a class as an AI agent.
 * <p>
 * An agent is a CDI bean that encapsulates autonomous, goal-driven behavior.
 * The agent's lifecycle, workflow, and actions are defined using additional annotations.
 * <p>
 * A name and description can be provided for documentation, discovery, or configuration.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
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
