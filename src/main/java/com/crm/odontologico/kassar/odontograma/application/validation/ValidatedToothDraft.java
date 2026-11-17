package com.crm.odontologico.kassar.odontograma.application.validation;

import com.crm.odontologico.kassar.odontograma.domain.model.CatalogoFdi.DienteFdiMetadata;
import com.crm.odontologico.kassar.odontograma.domain.model.EstadoClinico;

/**
 * Representación intermedia ya validada clínica y anatómicamente de un diente
 * extraído desde la respuesta parcial del modelo de IA o generado por defecto.
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

    /**
     * Crea un borrador con los estados por defecto ("Presente" a nivel general
     * y "Sano" en sus 5 superficies) para los dientes que el LLM no reportó.
     */
    public static ValidatedToothDraft porDefecto(DienteFdiMetadata metadata) {
        return new ValidatedToothDraft(
                metadata,
                EstadoClinico.PRESENTE,
                EstadoClinico.SANO,
                EstadoClinico.SANO,
                EstadoClinico.SANO,
                EstadoClinico.SANO,
                EstadoClinico.SANO
        );
    }

    /**
     * Crea un borrador cuando el LLM reporta un estado general sin detallar caras.
     * Si el estado es exclusivo de la raíz (ej. Fractura Radicular, Lesión Periapical),
     * las 5 caras coronales permanecen en {@link EstadoClinico#SANO}.
     */
    public static ValidatedToothDraft conEstadoCompleto(DienteFdiMetadata metadata, EstadoClinico estadoGeneral) {
        EstadoClinico estadoCaras = (estadoGeneral == EstadoClinico.PRESENTE || estadoGeneral.isExclusivoDeRaiz())
                ? EstadoClinico.SANO
                : estadoGeneral;
        return new ValidatedToothDraft(
                metadata,
                estadoGeneral,
                estadoCaras,
                estadoCaras,
                estadoCaras,
                estadoCaras,
                estadoCaras
        );
    }
}
