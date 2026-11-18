package com.crm.odontologico.kassar.odontograma.application.service;

import com.crm.odontologico.kassar.odontograma.application.dto.OdontogramaUpdateResponse;
import com.crm.odontologico.kassar.odontograma.application.dto.VoiceCaptureRequest;
import com.crm.odontologico.kassar.odontograma.application.mapper.OdontogramaDomainMapper;
import com.crm.odontologico.kassar.odontograma.application.port.AiChatMessage;
import com.crm.odontologico.kassar.odontograma.application.port.OdontogramaAiPort;
import com.crm.odontologico.kassar.odontograma.application.service.VoiceCaptureRequestNormalizer.NormalizedVoiceRequest;
import com.crm.odontologico.kassar.odontograma.application.validation.AiResponseValidator;
import com.crm.odontologico.kassar.odontograma.application.validation.ValidatedToothDraft;
import com.crm.odontologico.kassar.odontograma.application.validation.ValidationResult;
import com.crm.odontologico.kassar.odontograma.domain.exception.AiValidationExhaustedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Caso de uso central que orquesta el flujo de procesamiento de dictado por voz:
 * <ol>
 *   <li>Recibe la petición del Frontend y formatea el texto de entrada ({@link VoiceCaptureRequestNormalizer}).</li>
 *   <li>Envía el texto hacia el motor LLM ({@link OdontogramaAiPort}) para extraer exclusivamente los dientes mencionados.</li>
 *   <li>Valida el JSON parcial del LLM y autocorrige discrepancias anatómicas ({@link AiResponseValidator}).</li>
 *   <li>Consolida los dientes parciales con los 32 dientes de {@code CatalogoFdi}, asigna estados por defecto
 *       a los no reportados e inyecta los metadatos de UI ({@link OdontogramaDomainMapper}).</li>
 * </ol>
 */
@Service
public class ProcessVoiceDictationService {

    private static final Logger log = LoggerFactory.getLogger(ProcessVoiceDictationService.class);

    private final OdontogramaAiPort aiPort;
    private final OdontogramaPromptBuilder promptBuilder;
    private final AiResponseValidator responseValidator;
    private final VoiceCaptureRequestNormalizer requestNormalizer;
    private final OdontogramaDomainMapper domainMapper;
    private final int maxValidationAttempts;

    @Autowired
    public ProcessVoiceDictationService(
            OdontogramaAiPort aiPort,
            OdontogramaPromptBuilder promptBuilder,
            AiResponseValidator responseValidator,
            VoiceCaptureRequestNormalizer requestNormalizer,
            OdontogramaDomainMapper domainMapper,
            @Value("${odontograma.ai.max-attempts:3}") int maxValidationAttempts
    ) {
        this.aiPort = aiPort;
        this.promptBuilder = promptBuilder;
        this.responseValidator = responseValidator;
        this.requestNormalizer = requestNormalizer;
        this.domainMapper = domainMapper;
        this.maxValidationAttempts = Math.max(1, maxValidationAttempts);
    }

    public ProcessVoiceDictationService(
            OdontogramaAiPort aiPort,
            OdontogramaPromptBuilder promptBuilder,
            AiResponseValidator responseValidator,
            OdontogramaDomainMapper domainMapper,
            ObjectMapper objectMapper,
            int maxValidationAttempts
    ) {
        this(
                aiPort,
                promptBuilder,
                responseValidator,
                new VoiceCaptureRequestNormalizer(objectMapper),
                domainMapper,
                maxValidationAttempts
        );
    }

    public OdontogramaUpdateResponse procesarDictado(VoiceCaptureRequest rawRequest) {
        NormalizedVoiceRequest request = requestNormalizer.normalizar(rawRequest);
        List<ValidatedToothDraft> dientesReportados = obtenerDientesValidadosDelMotor(request);
        return domainMapper.toUpdateResponse(request.odontogramaId(), dientesReportados);
    }

    private List<ValidatedToothDraft> obtenerDientesValidadosDelMotor(NormalizedVoiceRequest request) {
        List<AiChatMessage> historial = inicializarHistorialConversacion(request.textoFormateado());
        List<String> ultimosErrores = List.of();
        String ultimaRespuestaCruda = "";

        for (int intento = 1; intento <= maxValidationAttempts; intento++) {
            ultimaRespuestaCruda = invocarMotorLlm(request.odontogramaId(), intento, historial);
            ValidationResult resultado = responseValidator.validar(ultimaRespuestaCruda);

            if (resultado.isValid()) {
                log.info("Respuesta parcial del LLM validada en intento {} ({} diente(s) reportado(s))",
                        intento, resultado.dientesValidados().size());
                return resultado.dientesValidados();
            }

            ultimosErrores = resultado.errores();
            log.warn("Intento {}/{} falló validación para odontogramaId='{}'. Errores: {}",
                    intento, maxValidationAttempts, request.odontogramaId(), ultimosErrores);

            agregarFeedbackDeCorreccionSiQuedanIntentos(intento, historial, ultimaRespuestaCruda, ultimosErrores);
        }

        throw new AiValidationExhaustedException(maxValidationAttempts, ultimosErrores, ultimaRespuestaCruda);
    }

    private List<AiChatMessage> inicializarHistorialConversacion(String textoFormateado) {
        List<AiChatMessage> historial = new ArrayList<>();
        historial.add(AiChatMessage.system(promptBuilder.obtenerSystemPrompt()));
        historial.add(AiChatMessage.user(promptBuilder.construirPromptUsuario(textoFormateado)));
        return historial;
    }

    private String invocarMotorLlm(String odontogramaId, int intento, List<AiChatMessage> historial) {
        log.info("Enviando texto al LLM para odontogramaId='{}' (intento {}/{})",
                odontogramaId, intento, maxValidationAttempts);

        long inicioNs = System.nanoTime();
        try {
            return aiPort.generarRespuestaJson(historial);
        } finally {
            long duracionMs = (System.nanoTime() - inicioNs) / 1_000_000;
            log.info("Duración de llamada al LLM para odontogramaId='{}' (intento {}/{}): {} ms",
                    odontogramaId, intento, maxValidationAttempts, duracionMs);
        }
    }

    private void agregarFeedbackDeCorreccionSiQuedanIntentos(
            int intentoActual,
            List<AiChatMessage> historial,
            String ultimaRespuestaCruda,
            List<String> errores
    ) {
        if (intentoActual < maxValidationAttempts) {
            historial.add(AiChatMessage.assistant(ultimaRespuestaCruda != null ? ultimaRespuestaCruda : ""));
            historial.add(AiChatMessage.user(promptBuilder.construirPromptCorreccion(errores)));
        }
    }
}
