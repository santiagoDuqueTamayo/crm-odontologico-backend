package com.crm.odontologico.kassar.odontograma.application.service;

import com.crm.odontologico.kassar.odontograma.domain.model.CatalogoFdi;
import com.crm.odontologico.kassar.odontograma.domain.model.EstadoClinico;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Construye los prompts clínicos estructurados para el modelo de IA y los mensajes
 * de retroalimentación (autocorrección) cuando una respuesta previa no supera la validación.
 */
@Component
public class OdontogramaPromptBuilder {

    private final String systemPrompt;

    public OdontogramaPromptBuilder() {
        this.systemPrompt = construirSystemPrompt();
    }

    public String obtenerSystemPrompt() {
        return systemPrompt;
    }

    public String construirPromptUsuario(String textoCapturado) {
        return "{\"textoCapturado\": \"%s\"}".formatted(
                textoCapturado.trim().replace("\"", "\\\""));
    }

    public String construirPromptCorreccion(List<String> erroresDetectados) {
        String listaErrores = erroresDetectados.stream()
                .map(err -> "- " + err)
                .collect(Collectors.joining("\n"));

        return """
                Corrige los siguientes errores en tu JSON anterior y devuelve SOLO el JSON válido:
                %s
                """.formatted(listaErrores);
    }

    private String construirSystemPrompt() {
        return """
                Convierte el dictado odontológico a JSON válido sin markdown.
                Usa nomenclatura FDI (11-18, 21-28, 31-38, 41-48; posiciones: 1=incisivo central, 2=lateral, 3=canino, 4=1er premolar, 5=2do premolar, 6=1er molar, 7=2do molar, 8=3er molar; cuadrantes: 1=sup der, 2=sup izq, 3=inf izq, 4=inf der).
                Estados permitidos: [%s].
                Si dice "el resto sano", asigna "Sano" a las demás caras y "Presente" a estadoGeneral.
                Formato: {"dientes":[{"numero":15,"estadoGeneral":"Presente","caras":{"mesial":"Sano","distal":"Sano","vestibular":"Sano","palatino_lingual":"Sano","oclusal_incisal":"Caries"}}]}
                """.formatted(EstadoClinico.valoresPermitidosComoTexto());
    }
}
