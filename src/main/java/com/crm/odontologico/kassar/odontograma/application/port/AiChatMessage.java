package com.crm.odontologico.kassar.odontograma.application.port;

/**
 * Mensaje individual dentro del historial de conversación con el modelo de IA,
 * permitiendo mantener el contexto durante el bucle de autocorrección.
 */
public record AiChatMessage(
        Role role,
        String content
) {
    public enum Role {
        SYSTEM("system"),
        USER("user"),
        ASSISTANT("assistant");

        private final String value;

        Role(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public static AiChatMessage system(String content) {
        return new AiChatMessage(Role.SYSTEM, content);
    }

    public static AiChatMessage user(String content) {
        return new AiChatMessage(Role.USER, content);
    }

    public static AiChatMessage assistant(String content) {
        return new AiChatMessage(Role.ASSISTANT, content);
    }
}
