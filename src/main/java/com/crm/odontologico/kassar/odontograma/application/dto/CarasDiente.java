package com.crm.odontologico.kassar.odontograma.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Agrupa las 5 caras exactas que envía el Backend hacia el Frontend.
 * Mantiene las llaves JSON estables ({@code palatino_lingual} y {@code oclusal_incisal})
 * mientras que cada {@link CaraVisual} contiene su {@code etiquetaVisual} anatómica exacta
 * según el cuadrante y grupo dental del diente FDI.
 */
public record CarasDiente(
        CaraVisual mesial,
        CaraVisual distal,
        CaraVisual vestibular,
        @JsonProperty("palatino_lingual") CaraVisual palatinoLingual,
        @JsonProperty("oclusal_incisal") CaraVisual oclusalIncisal
) {
}
