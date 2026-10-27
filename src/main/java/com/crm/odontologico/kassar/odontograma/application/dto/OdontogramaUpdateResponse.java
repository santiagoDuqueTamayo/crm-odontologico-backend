package com.crm.odontologico.kassar.odontograma.application.dto;

import java.util.List;

/**
 * Contrato de Salida (Backend -> Frontend) tras procesar y validar el dictado con IA.
 * Soporta múltiples dientes en una misma frase dictada por el odontólogo.
 */
public record OdontogramaUpdateResponse(
        String odontogramaId,
        List<DienteData> dientes
) {
}
