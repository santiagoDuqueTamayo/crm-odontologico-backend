package com.crm.odontologico.kassar.odontograma.infrastructure.ai.ollama;

import com.crm.odontologico.kassar.odontograma.application.port.AiChatMessage;
import com.crm.odontologico.kassar.odontograma.application.port.OdontogramaAiPort;
import com.crm.odontologico.kassar.odontograma.domain.exception.AiProviderCommunicationException;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

/**
 * Adaptador de infraestructura que implementa {@link OdontogramaAiPort} conectándose
 * a la API de NVIDIA NIM ({@code POST /v1/chat/completions}).
 */
@Component
@ConditionalOnProperty(prefix = "odontograma.ai", name = "provider", havingValue = "nvidia-nim")
public class NvidiaNimAiAdapter implements OdontogramaAiPort {

    private static final Logger log = LoggerFactory.getLogger(NvidiaNimAiAdapter.class);

    private final RestClient nvidiaRestClient;
    private final NvidiaNimProperties properties;
    private final ObjectMapper objectMapper;

    @Autowired
    public NvidiaNimAiAdapter(
            RestClient nvidiaRestClient,
            NvidiaNimProperties properties,
            ObjectMapper objectMapper
    ) {
        this.nvidiaRestClient = nvidiaRestClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public NvidiaNimAiAdapter(RestClient nvidiaRestClient, NvidiaNimProperties properties) {
        this(nvidiaRestClient, properties, JsonMapper.builder().build());
    }

    @Override
    public String generarRespuestaJson(List<AiChatMessage> messages) {
        try {
            validarApiKeyConfigurada();

            NvidiaChatRequest requestPayload = construirPayload(messages);
            log.debug("Enviando petición a NVIDIA NIM ({}{}) con modelo '{}' y {} mensaje(s)",
                    properties.baseUrl(), properties.chatEndpoint(), properties.model(), messages.size());

            String rawBody = ejecutarPeticionHttp(requestPayload);
            return extraerContenidoDeRespuesta(rawBody);

        } catch (AiProviderCommunicationException ex) {
            log.error("Falló la llamada a NVIDIA NIM (modelo '{}').\n{}",
                    properties.model(), trazaSegura(ex));
            throw ex;
        } catch (JacksonException ex) {
            log.error("NVIDIA NIM devolvió una respuesta JSON inválida (modelo '{}').\n{}",
                    properties.model(), trazaSegura(ex));
            throw new AiProviderCommunicationException(
                    "Respuesta JSON inválida desde NVIDIA NIM (modelo: " + properties.model() + "): "
                            + redactarClave(ex.getOriginalMessage()),
                    ex
            );
        } catch (RestClientException ex) {
            String safeMsg = redactarClave(ex.getMessage());
            log.error("Error comunicándose con NVIDIA NIM (modelo '{}'): {}",
                    properties.model(), safeMsg + "\n" + trazaSegura(ex));
            throw new AiProviderCommunicationException(
                    "Error al comunicarse con la API de NVIDIA NIM (modelo: " + properties.model() + "): " + safeMsg,
                    ex
            );
        }
    }

    private void validarApiKeyConfigurada() {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new AiProviderCommunicationException(
                    "La clave de API de NVIDIA NIM ('odontograma.ai.nvidia.api-key') no está configurada.");
        }
    }

    private NvidiaChatRequest construirPayload(List<AiChatMessage> messages) {
        List<NvidiaMessagePayload> nvidiaMessages = messages.stream()
                .map(msg -> new NvidiaMessagePayload(msg.role().getValue(), msg.content()))
                .toList();

        return new NvidiaChatRequest(
                properties.model(),
                nvidiaMessages,
                properties.temperature(),
                properties.maxTokens(),
                false
        );
    }

    private String ejecutarPeticionHttp(NvidiaChatRequest requestPayload) {
        String rawBody = nvidiaRestClient.post()
                .uri(properties.chatEndpoint())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey().trim())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(requestPayload)
                .retrieve()
                .body(String.class);

        if (rawBody == null || rawBody.isBlank()) {
            throw new AiProviderCommunicationException("NVIDIA NIM devolvió una respuesta vacía.");
        }
        return rawBody;
    }

    private String extraerContenidoDeRespuesta(String rawBody) {
        NvidiaChatResponse response = objectMapper.readValue(rawBody, NvidiaChatResponse.class);

        if (response.choices() != null && !response.choices().isEmpty()) {
            NvidiaChoice firstChoice = response.choices().get(0);
            if (firstChoice != null && firstChoice.message() != null) {
                String content = firstChoice.message().content();
                if (content != null && !content.isBlank()) {
                    return content;
                }

                // Si el modelo agotó max_tokens durante reasoning_content (finish_reason = "length")
                if ("length".equalsIgnoreCase(firstChoice.finishReason())) {
                    throw new AiProviderCommunicationException(
                            "El modelo de NVIDIA NIM agotó el límite de tokens (max_tokens="
                                    + properties.maxTokens()
                                    + ") durante su razonamiento interno ('reasoning_content') antes de emitir el JSON final.");
                }
            }
        }

        throw new AiProviderCommunicationException(
                "La respuesta de NVIDIA NIM no contiene contenido en 'choices[0].message.content'.");
    }

    private String redactarClave(String mensaje) {
        if (mensaje == null) {
            return "sin detalle";
        }
        String key = properties.apiKey();
        if (key == null || key.isBlank()) {
            return mensaje;
        }
        return mensaje.replace(key, "[REDACTED]");
    }

    private String trazaSegura(Throwable exception) {
        StringWriter trace = new StringWriter();
        exception.printStackTrace(new PrintWriter(trace));
        return redactarClave(trace.toString());
    }

    public record NvidiaChatRequest(
            String model,
            List<NvidiaMessagePayload> messages,
            double temperature,
            @JsonProperty("max_tokens") int maxTokens,
            boolean stream
    ) {
    }

    public record NvidiaMessagePayload(
            String role,
            String content
    ) {
    }

    public record NvidiaResponseMessage(
            String role,
            String content,
            @JsonProperty("reasoning_content") String reasoningContent
    ) {
    }

    public record NvidiaChatResponse(
            String id,
            String model,
            List<NvidiaChoice> choices
    ) {
    }

    public record NvidiaChoice(
            Integer index,
            NvidiaResponseMessage message,
            @JsonProperty("finish_reason") String finishReason
    ) {
    }
}
