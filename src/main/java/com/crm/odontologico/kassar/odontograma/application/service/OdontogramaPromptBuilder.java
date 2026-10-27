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
        return """
                Interpreta el siguiente dictado clínico odontológico y devuelve EXCLUSIVAMENTE el objeto JSON estructurado:
                "%s"
                """.formatted(textoCapturado.trim());
    }

    public String construirPromptCorreccion(List<String> erroresDetectados) {
        String listaErrores = erroresDetectados.stream()
                .map(err -> "- " + err)
                .collect(Collectors.joining("\n"));

        return """
                Tu respuesta anterior NO superó las validaciones clínicas y estructurales del Backend por los siguientes motivos:
                %s

                Corrige todos los errores listados y responde ÚNICAMENTE con el objeto JSON válido siguiendo exactamente el esquema solicitado.
                """.formatted(listaErrores);
    }

    private String construirSystemPrompt() {
        String tablaFdi = CatalogoFdi.obtenerTodos().values().stream()
                .map(meta -> "  - %d: %s (Cara interna: %s | Cara masticatoria/borde: %s)".formatted(
                        meta.numero(),
                        meta.nombreColoquial(),
                        meta.etiquetaCaraInterna(),
                        meta.etiquetaCaraMasticatoria()))
                .collect(Collectors.joining("\n"));

        return """
                Eres un asistente experto en odontología clínica especializado en estructurar dictados de voz desordenados hacia un Odontograma estándar FDI de 32 dientes permanentes.

                1. CATÁLOGO OFICIAL FDI DE 32 DIENTES Y ANATOMÍA DE CARAS:
                %s

                2. ESTADOS CLÍNICOS PERMITIDOS (Catálogo cerrado):
                [%s]

                3. REGLAS DE INTERPRETACIÓN CLÍNICA:
                - El odontólogo puede dictar por número FDI (ej. "diente 15") o por nombre coloquial (ej. "segundo premolar superior derecho" -> 15, "primer molar inferior izquierdo" -> 36).
                - El dictado puede mencionar uno o varios dientes en la misma frase; inclúyelos todos en el arreglo "dientes".
                - Si el odontólogo menciona hallazgos en caras específicas y dice "el resto sano" (o no menciona las demás caras de un diente presente), asigna "Sano" a las caras no mencionadas y "Presente" en "estadoGeneral".
                - Si el diente completo está "Ausente", "Extraído", "Retenido" o tiene "Implante", coloca ese estado en "estadoGeneral" y también en sus 5 caras.
                - Respeta estrictamente la anatomía:
                  * Dientes superiores (11-28) usan cara interna "Palatino" en la llave "palatino_lingual".
                  * Dientes inferiores (31-48) usan cara interna "Lingual" en la llave "palatino_lingual".
                  * Dientes anteriores (11-13, 21-23, 31-33, 41-43: incisivos y caninos) usan borde "Incisal" en la llave "oclusal_incisal".
                  * Dientes posteriores (14-18, 24-28, 34-38, 44-48: premolares y molares) usan cara "Oclusal" en la llave "oclusal_incisal".

                4. FORMATO DE SALIDA (RESPONDE EXCLUSIVAMENTE CON JSON VÁLIDO, SIN MARKDOWN NI EXPLICACIONES):
                {
                  "dientes": [
                    {
                      "numero": 15,
                      "estadoGeneral": { "estado": "Presente" },
                      "caras": {
                        "mesial": { "etiquetaVisual": "Mesial", "estado": "Sano" },
                        "distal": { "etiquetaVisual": "Distal", "estado": "Sano" },
                        "vestibular": { "etiquetaVisual": "Vestibular", "estado": "Sano" },
                        "palatino_lingual": { "etiquetaVisual": "Palatino", "estado": "Sano" },
                        "oclusal_incisal": { "etiquetaVisual": "Oclusal", "estado": "Caries" }
                      }
                    }
                  ]
                }
                """.formatted(tablaFdi, EstadoClinico.valoresPermitidosComoTexto());
    }
}
