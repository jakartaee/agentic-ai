package jakarta.ai.agent;

/**
 * Minimal facade for Large Language Model (LLM) operations.
 * <p>
 * Intended to be injected via CDI into agents. Provides a unified interface for querying LLMs
 * with support for type conversion of parameters and results.
 * <p>
 * Implementations will delegate to external LLM APIs or services.
 */

public interface LargeLanguageModel {

    /**
     * Sends a prompt to the model and returns a String response.
     * <p>
     * This is the simplest form of LLM interaction, suitable for plain text prompts and responses.
     *
     * @param prompt The input prompt or question.
     * @return The model's response as a String.
     */
    String query(String prompt);

    /**
     * Sends a prompt and a variable number of input objects to the model, returning a String response.
     * <p>
     * The input objects may be domain objects, JSON, or other serializable types. Implementations
     * should handle conversion as needed.
     *
     * @param prompt The prompt or query.
     * @param inputs The input objects (e.g., domain objects, JSON, etc.).
     * @return The model's response as a String.
     */
    String query(String prompt, Object... inputs);

    /**
     * Sends a prompt and a variable number of input objects to the model, returning a response of the specified type.
     * <p>
     * The result is converted to the requested type if supported by the implementation.
     *
     * @param prompt The prompt or query.
     * @param resultType The expected result type.
     * @param inputs The input objects.
     * @param <T> The type of the result.
     * @return The model's response converted to the specified type.
     */
    <T> T query(String prompt, Class<T> resultType, Object... inputs);

    /**
     * Sends a prompt to the model and returns a response of the specified type.
     * <p>
     * The result is converted to the requested type if supported by the implementation.
     *
     * @param prompt The prompt or query.
     * @param resultType The expected result type.
     * @param <T> The type of the result.
     * @return The model's response converted to the specified type.
     */
    <T> T query(String prompt, Class<T> resultType);    
}
