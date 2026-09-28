package StockPlus.demo.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class HttpLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(HttpLlmClient.class);

    private final String baseUrl;
    private final String model;
    private final String apiKey;
    private final String productHeader;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public HttpLlmClient(
            @Value("${llm.base-url:https://litellm-qc.zycus.net/v1}") String baseUrl,
            @Value("${llm.model:qwen-cursor}") String model,
            @Value("${llm.api-key:}") String apiKey,
            @Value("${llm.product-header:PC1}") String productHeader,
            @Value("${llm.timeout-seconds:15}") int timeoutSeconds,
            ObjectMapper objectMapper) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.model = model;
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.productHeader = productHeader;
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(timeoutSeconds));
        requestFactory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl(this.baseUrl)
                .build();
    }

    @Override
    public String chatCompletion(String systemPrompt, String userPrompt) {
        if (apiKey.isEmpty()) {
            throw new IllegalStateException("LLM API key is not configured in environment variable LLM_API_KEY");
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("model", model);
            payload.put("temperature", 0.2);

            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> sysMsg = new HashMap<>();
            sysMsg.put("role", "system");
            sysMsg.put("content", systemPrompt);
            messages.add(sysMsg);

            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userPrompt);
            messages.add(userMsg);

            payload.put("messages", messages);

            String requestBody = objectMapper.writeValueAsString(payload);

            String responseBody = restClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .header("product", productHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            if (responseBody == null || responseBody.isBlank()) {
                throw new IllegalStateException("Empty response received from LLM endpoint");
            }

            JsonNode rootNode = objectMapper.readTree(responseBody);
            JsonNode choices = rootNode.get("choices");
            if (choices != null && choices.isArray() && !choices.isEmpty()) {
                JsonNode message = choices.get(0).get("message");
                if (message != null && message.has("content")) {
                    return message.get("content").asText();
                }
            }

            throw new IllegalStateException("Malformed response from LLM: missing choices[0].message.content");

        } catch (Exception e) {
            log.warn("LLM chat completion call failed: {}", e.getMessage());
            throw new RuntimeException("LLM call failed: " + e.getMessage(), e);
        }
    }
}
