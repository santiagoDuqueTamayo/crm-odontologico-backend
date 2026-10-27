package com.crm.odontologico.kassar.odontograma.domain.exception;

import java.util.List;

/**
 * Excepción lanzada cuando el modelo de IA no logra generar una respuesta clínica válida
 * tras agotar el número máximo de intentos de autocorrección.
 */
public class AiValidationExhaustedException extends RuntimeException {

    private final int intentosRealizados;
    private final List<String> ultimosErrores;
    private final String ultimaRespuestaCruda;

    public AiValidationExhaustedException(
            int intentosRealizados,
            List<String> ultimosErrores,
            String ultimaRespuestaCruda
    ) {
        super("El modelo de IA no pudo generar un odontograma válido tras " + intentosRealizados
                + " intentos. Errores pendientes: " + String.join("; ", ultimosErrores));
        this.intentosRealizados = intentosRealizados;
        this.ultimosErrores = List.copyOf(ultimosErrores);
        this.ultimaRespuestaCruda = ultimaRespuestaCruda;
    }

    public int getIntentosRealizados() {
        return intentosRealizados;
    }

    public List<String> getUltimosErrores() {
        return ultimosErrores;
    }

    public String getUltimaRespuestaCruda() {
        return ultimaRespuestaCruda;
    }
}
