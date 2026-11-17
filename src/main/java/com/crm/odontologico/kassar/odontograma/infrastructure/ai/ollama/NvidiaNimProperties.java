package com.crm.odontologico.kassar.odontograma.infrastructure.ai.ollama;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades externalizadas para la conexión con la API de NVIDIA NIM (compatible con OpenAI Chat Completions).
 */
@ConfigurationProperties(prefix = "odontograma.ai.nvidia")
public record NvidiaNimProperties(
        String baseUrl,
        String chatEndpoint,
        String apiKey,
        String model,
        double temperature,
        int maxTokens,
        int connectTimeoutSeconds,
        int readTimeoutSeconds
) {
    public NvidiaNimProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://integrate.api.nvidia.com/v1";
        }
        if (chatEndpoint == null || chatEndpoint.isBlank()) {
            chatEndpoint = "/chat/completions";
        }
        if (model == null || model.isBlank()) {
            model = "mistralai/mistral-large-4-1t-a52b";
        }
        if (maxTokens <= 0) {
            maxTokens = 4096;
        }
        if (connectTimeoutSeconds <= 0) {
            connectTimeoutSeconds = 15;
        }
        if (readTimeoutSeconds <= 0) {
            readTimeoutSeconds = 120;
        }
    }
}
