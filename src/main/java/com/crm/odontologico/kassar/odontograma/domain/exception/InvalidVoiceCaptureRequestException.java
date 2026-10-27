package com.crm.odontologico.kassar.odontograma.domain.exception;

/**
 * Excepción lanzada cuando la petición de entrada desde el Frontend no contiene los datos mínimos requeridos.
 */
public class InvalidVoiceCaptureRequestException extends RuntimeException {

    public InvalidVoiceCaptureRequestException(String message) {
        super(message);
    }
}
