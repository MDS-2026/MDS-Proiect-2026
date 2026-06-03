package com.mdsproject.backend.config;

/**
 * Normalizes {@code ai.endpoint} for LangChain4j OpenAI client (expects base URL ending in /v1).
 */
public final class AiEndpointSupport {

    private AiEndpointSupport() {
    }

    public static String normalizeOpenAiBaseUrl(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) {
            return "https://api.openai.com/v1";
        }
        String url = endpoint.trim();
        if (url.endsWith("/chat/completions")) {
            url = url.substring(0, url.length() - "/chat/completions".length());
        }
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        if (!url.endsWith("/v1")
                && !url.contains("/openai")
                && !url.contains("/v1beta")) {
            url = url + "/v1";
        }
        return url;
    }
}
