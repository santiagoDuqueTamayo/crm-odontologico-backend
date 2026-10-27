package com.crm.odontologico.kassar.odontograma.application.validation;

import java.util.List;

/**
 * Resultado inmutable del proceso de validación de la respuesta generada por el modelo de IA.
 */
public record ValidationResult(
        List<ValidatedToothDraft> dientesValidados,
        List<String> errores
) {
    public boolean isValid() {
        return errores.isEmpty() && !dientesValidados.isEmpty();
    }
}
