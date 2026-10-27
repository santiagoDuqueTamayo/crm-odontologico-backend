package com.crm.odontologico.kassar.odontograma.application.dto;

import com.crm.odontologico.kassar.odontograma.domain.model.EstadoClinico;

/**
 * Representa el estado individual de una de las 5 caras del diente hacia el Frontend.
 *
 * @param etiquetaVisual Nombre anatómico exacto ("Mesial", "Distal", "Vestibular", "Palatino"/"Lingual", "Oclusal"/"Incisal")
 * @param estado         Estado clínico validado del catálogo cerrado
 * @param colorHex       Código de color hexadecimal asociado al estado (o null si no aplica)
 */
public record CaraVisual(
        String etiquetaVisual,
        EstadoClinico estado,
        String colorHex
) {
    public static CaraVisual de(String etiquetaVisual, EstadoClinico estado) {
        return new CaraVisual(etiquetaVisual, estado, estado.getColorHex());
    }
}
