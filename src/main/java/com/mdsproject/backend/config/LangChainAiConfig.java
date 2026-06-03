package com.mdsproject.backend.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class LangChainAiConfig {

    @Bean
    ChatLanguageModel chatLanguageModel(
            @Value("${ai.endpoint:}") String endpoint,
            @Value("${ai.api-key:}") String apiKey,
            @Value("${ai.model:gemini-2.5-flash}") String model,
            @Value("${ai.timeout-ms:30000}") long timeoutMs) {

        String baseUrl = AiEndpointSupport.normalizeOpenAiBaseUrl(endpoint);

        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey == null || apiKey.isBlank() ? "unused" : apiKey)
                .modelName(model)
                .temperature(0.4)
                .timeout(Duration.ofMillis(timeoutMs))
                .logRequests(false)
                .logResponses(false)
                .build();
    }
}
