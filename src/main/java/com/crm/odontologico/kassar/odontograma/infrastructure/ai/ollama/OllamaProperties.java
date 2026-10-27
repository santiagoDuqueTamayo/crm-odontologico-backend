package com.crm.odontologico.kassar.odontograma.infrastructure.ai.ollama;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades externalizadas para la conexión con el servidor local de Ollama.
 */
@ConfigurationProperties(prefix = "odontograma.ai.ollama")
public record OllamaProperties(
        String baseUrl,
        String chatEndpoint,
        String model,
        double temperature,
        int connectTimeoutSeconds,
        int readTimeoutSeconds
) {
    public OllamaProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "http://localhost:11434";
        }
        if (chatEndpoint == null || chatEndpoint.isBlank()) {
            chatEndpoint = "/api/chat";
        }
        if (model == null || model.isBlank()) {
            model = "llama3.2";
        }
        if (connectTimeoutSeconds <= 0) {
            connectTimeoutSeconds = 10;
        }
        if (readTimeoutSeconds <= 0) {
            readTimeoutSeconds = 60;
        }
    }
}
