package com.crm.odontologico.kassar.odontograma.application.validation;

import com.crm.odontologico.kassar.odontograma.domain.model.CatalogoFdi.DienteFdiMetadata;
import com.crm.odontologico.kassar.odontograma.domain.model.EstadoClinico;

/**
 * Representación intermedia ya validada clínica y anatómicamente de un diente
 * extraído desde la respuesta del modelo de IA.
 */
public record ValidatedToothDraft(
        DienteFdiMetadata metadata,
        EstadoClinico estadoGeneral,
        EstadoClinico mesial,
        EstadoClinico distal,
        EstadoClinico vestibular,
        EstadoClinico palatinoLingual,
        EstadoClinico oclusalIncisal
) {
}
