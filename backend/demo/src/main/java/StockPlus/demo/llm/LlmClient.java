package StockPlus.demo.llm;

public interface LlmClient {
    /**
     * Executes a chat completion request to the LLM.
     *
     * @param systemPrompt System instructions for the advisor
     * @param userPrompt   User prompt with domain signals
     * @return Raw content returned by the LLM
     */
    String chatCompletion(String systemPrompt, String userPrompt);
}
