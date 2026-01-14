package jakarta.ai.agent;

/**
 * Minimal facade for Large Language Model (LLM) operations.
 * <p>
 * Intended to be injected via CDI into agents. Provides a unified interface for querying LLMs
 * with support for type conversion of parameters and results.
 * <p>
 * Implementations will delegate to external LLM APIs or services.
 *
 * @param <T> The type parameter for the model implementation
 */
public interface LargeLanguageModel<T> {

    /**
     * Gets the default model instance for this LLM provider.
     * <p>
     * This method returns a configured instance of the LLM that can be used
     * for standard operations without additional configuration.
     *
     * @return The default model instance
     */
    LargeLanguageModel<T> getDefaultModel();

    /**
     * Invokes the large language model with a prompt and an input object.
     * <p>
     * The input object may be a domain object, JSON, or other serializable type.
     * Implementations should handle conversion as needed. The response format
     * depends on the implementation and model configuration.
     *
     * @param prompt The input prompt or question
     * @param object The input object to be processed along with the prompt
     * @return The model's response, typically as a String or structured object
     */
    Object invokeLargeLanguageModel(String prompt, Object object);

    /**
     * Gets the underlying LLM implementation.
     * <p>
     * This allows access to vendor-specific APIs or advanced features not exposed
     * by the facade. The returned type depends on the specific LLM provider
     * implementation being used.
     *
     * @return The underlying LLM implementation instance
     */
    Object getUnderlyingLlm();
}
