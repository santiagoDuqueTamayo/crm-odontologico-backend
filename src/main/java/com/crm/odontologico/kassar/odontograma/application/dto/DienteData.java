package com.crm.odontologico.kassar.odontograma.application.dto;

import com.crm.odontologico.kassar.odontograma.domain.model.GrupoDental;

/**
 * Estructura central de un Diente enriquecida con su clasificación anatómica FDI.
 */
public record DienteData(
        int numero,
        String nombreColoquial,
        int cuadrante,
        GrupoDental grupoDental,
        EstadoGeneral estadoGeneral,
        CarasDiente caras
) {
}
