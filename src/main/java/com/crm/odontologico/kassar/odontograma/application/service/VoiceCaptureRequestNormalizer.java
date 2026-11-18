package com.crm.odontologico.kassar.odontograma.application.service;

import com.crm.odontologico.kassar.odontograma.application.dto.VoiceCaptureRequest;
import com.crm.odontologico.kassar.odontograma.domain.exception.InvalidVoiceCaptureRequestException;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Responsable de recibir la petición del Frontend, extraer sus campos obligatorios
 * (soportando tanto el contrato directo como el envuelto en {@code prompt}) y formatear
 * el texto de entrada antes de enviarlo al motor de IA.
 */
@Component
public class VoiceCaptureRequestNormalizer {

    private final ObjectMapper objectMapper;

    public VoiceCaptureRequestNormalizer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public NormalizedVoiceRequest normalizar(VoiceCaptureRequest rawRequest) {
        if (rawRequest == null) {
            throw new InvalidVoiceCaptureRequestException("El cuerpo de la petición no puede ser nulo.");
        }

        VoiceCaptureRequest unwrapped = desenrollarPromptSiAplica(rawRequest);
        validarCamposObligatorios(unwrapped);

        String odontogramaId = unwrapped.odontogramaId().trim();
        String textoFormateado = formatearTextoEntrada(unwrapped.textoCapturado());

        return new NormalizedVoiceRequest(odontogramaId, textoFormateado);
    }

    private VoiceCaptureRequest desenrollarPromptSiAplica(VoiceCaptureRequest rawRequest) {
        if (!requiereDesenrollarPrompt(rawRequest)) {
            return rawRequest;
        }

        try {
            JsonNode promptNode = objectMapper.readTree(rawRequest.prompt());
            if (promptNode == null || !promptNode.isObject()) {
                return rawRequest;
            }
            String odontogramaId = extraerTexto(promptNode, "odontogramaId", rawRequest.odontogramaId());
            String textoCapturado = extraerTexto(promptNode, "textoCapturado", rawRequest.textoCapturado());
            return new VoiceCaptureRequest(odontogramaId, textoCapturado, rawRequest.prompt());
        } catch (JacksonException ex) {
            throw new InvalidVoiceCaptureRequestException(
                    "El campo 'prompt' no contiene un JSON válido con 'odontogramaId' y 'textoCapturado'.");
        }
    }

    private boolean requiereDesenrollarPrompt(VoiceCaptureRequest request) {
        boolean faltaCampoPrincipal = esVacio(request.odontogramaId()) || esVacio(request.textoCapturado());
        return faltaCampoPrincipal && !esVacio(request.prompt());
    }

    private String extraerTexto(JsonNode node, String fieldName, String valorActual) {
        if (node.hasNonNull(fieldName)) {
            return node.get(fieldName).asString();
        }
        return valorActual;
    }

    private void validarCamposObligatorios(VoiceCaptureRequest request) {
        if (esVacio(request.odontogramaId())) {
            throw new InvalidVoiceCaptureRequestException("El campo 'odontogramaId' es obligatorio.");
        }
        if (esVacio(request.textoCapturado())) {
            throw new InvalidVoiceCaptureRequestException(
                    "El campo 'textoCapturado' es obligatorio y no puede estar vacío.");
        }
    }

    private String formatearTextoEntrada(String rawText) {
        return rawText
                .replaceAll("[\\r\\n\\t]+", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();
    }

    private boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }

    public record NormalizedVoiceRequest(String odontogramaId, String textoFormateado) {
    }
}
