package com.crm.odontologico.kassar.odontograma.application.port;

import java.util.List;

/**
 * Puerto de salida (Hexagonal Architecture) para la comunicación con el motor de IA.
 * Desacopla la lógica de negocio del proveedor específico (Ollama, OpenAI, vLLM, etc.).
 */
public interface OdontogramaAiPort {

    /**
     * Envía el historial de mensajes al modelo de IA y retorna la respuesta en texto/JSON crudo.
     *
     * @param messages Historial de mensajes (System prompt, dictado del usuario y posibles correcciones)
     * @return Respuesta generada por el modelo de IA
     */
    String generarRespuestaJson(List<AiChatMessage> messages);
}
