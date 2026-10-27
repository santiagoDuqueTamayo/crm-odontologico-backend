package com.crm.odontologico.kassar.odontograma.domain.exception;

/**
 * Excepción lanzada cuando falla la conexión o comunicación con el servidor que expone el modelo de IA.
 */
public class AiProviderCommunicationException extends RuntimeException {

    public AiProviderCommunicationException(String message, Throwable cause) {
        super(message, cause);
    }

    public AiProviderCommunicationException(String message) {
        super(message);
    }
}
