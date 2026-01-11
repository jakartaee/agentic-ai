package jakarta.ai.agent;

/**
 * Represents the result of a decision or workflow step in an agent.
 * <p>
 * Used to standardize workflow branching and outcome handling.
 * The 'success' flag indicates a positive or negative result, and 'details' can hold
 * additional information, such as error messages, domain objects, or context.
 */
public record Result(boolean success, Object details) {}
