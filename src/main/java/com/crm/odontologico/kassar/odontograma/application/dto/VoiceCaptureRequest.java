package com.crm.odontologico.kassar.odontograma.application.dto;

/**
 * Contrato de Entrada (Frontend -> Backend) cuando el micrófono termina de capturar la frase.
 * Soporta tanto el formato directo {@code {"odontogramaId": "...", "textoCapturado": "..."}}
 * como el formato envuelto {@code {"prompt": "{\"odontogramaId\": ...}"}}.
 */
public record VoiceCaptureRequest(
        String odontogramaId,
        String textoCapturado,
        String prompt
) {
    public VoiceCaptureRequest(String odontogramaId, String textoCapturado) {
        this(odontogramaId, textoCapturado, null);
    }
}
