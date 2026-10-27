package com.crm.odontologico.kassar.odontograma.application.dto;

import com.crm.odontologico.kassar.odontograma.domain.model.EstadoClinico;

/**
 * Representa el estado a nivel del diente completo (ej. Presente, Ausente, Extraído, Implante).
 */
public record EstadoGeneral(
        EstadoClinico estado,
        String colorHex
) {
    public static EstadoGeneral de(EstadoClinico estado) {
        return new EstadoGeneral(estado, estado.getColorHex());
    }
}
