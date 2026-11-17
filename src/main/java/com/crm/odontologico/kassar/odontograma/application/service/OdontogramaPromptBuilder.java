package com.crm.odontologico.kassar.odontograma.application.service;

import com.crm.odontologico.kassar.odontograma.domain.model.EstadoClinico;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Construye los prompts clínicos estructurados para el motor de IA y los mensajes
 * de retroalimentación cuando la respuesta no cumple con el esquema JSON o catálogo FDI.
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
        return textoCapturado.trim();
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
                Convierte el dictado odontológico en un único objeto JSON parcial y simplificado, sin bloques markdown.
                Razona de manera extremadamente breve y directa sin divagar.
                Reglas de responsabilidad del modelo:
                1. Extrae EXCLUSIVAMENTE los dientes que el odontólogo menciona explícitamente en el texto (NO completes los 32 dientes; ignora frases como "resto de dientes sano").
                2. Devuelve un JSON simplificado SIN colores (colorHex) y SIN nombres coloquiales (nombreColoquial).
                3. En cada diente mencionado:
                   - Si es un estado de DIENTE COMPLETO (Endodoncia, Núcleo/Poste, Corona, Póntico, Prótesis Removible, Implante, Ausente, Extraído, Resto Radicular, Retenido, En Erupción, o Sano general) o si se menciona un hallazgo sin especificar cara (ej. "11 sano", "13 ausente", "15 amalgama fracturada"), asígnalo directamente en "estadoGeneral" sin incluir "caras".
                   - Si el hallazgo afecta caras específicas de un diente presente, pon "estadoGeneral": "Presente" y en "caras" asigna el estado a las caras afectadas y "Sano" a las demás.
                4. Mapa FDI permitido: 11-18, 21-28, 31-38, 41-48 (cuadrantes: 1=sup der, 2=sup izq, 3=inf izq, 4=inf der; posiciones: 1=incisivo central/paleta, 2=lateral, 3=canino/colmillo, 4=1er premolar, 5=2do premolar, 6=1er molar, 7=2do molar, 8=3er molar/cordal).
                5. Caras (5 llaves): "mesial", "distal", "vestibular" (labial/bucal/cervical), "palatino_lingual" (palatina/lingual), "oclusal_incisal" (oclusal/incisal). Compuestos: ocluso-mesial=oclusal_incisal+mesial; ocluso-distal=oclusal_incisal+distal; MOD=mesial+oclusal_incisal+distal.
                6. Estados permitidos exactos: [%s].
                7. Agrupación de sinónimos clínicos hacia el estado oficial:
                   [SALUD Y PREVENCIÓN]
                   - Sano, intacto, cero, código 0, sin patología, normal -> "Sano".
                   - Presente (estado general neutro cuando hay hallazgos por caras) -> "Presente".
                   - Sellante, sellador, sellante de fosas y fisuras, sellante preventivo -> "Sellante".
                   [PATOLOGÍAS Y DAÑOS (Por caras o general)]
                   - Caries, lesión cariosa, mancha blanca/marrón, descalcificación, caries incipiente, ICDAS 1-6, caries microcavitada/dentinaria/cavitada/abierta/profunda/recidivante/secundaria, hueco, caribe -> "Caries".
                   - Fracturado, fractura coronal/incisal/esmalte/dentina, diente roto, astillado, fisura dental -> "Fracturado".
                   - Desgaste, abfracción, bruxismo, atrición, abrasión, erosión, cepillado agresivo, lesión cervical no cariosa (LCNC), cuello destapado, raya cervical, faceta de desgaste -> "Desgaste/Abfracción".
                   - Restauración defectuosa, resina/amalgama filtrada o microfiltrada, amalgama/resina fracturada o rota, restauración caída/desalojada, desadaptada, márgenes abiertos, sobrecontorneada, tinción marginal, para cambio -> "Rest. Defectuosa".
                   [TRATAMIENTOS RESTAURATIVOS (Por caras)]
                   - Restaurado, resina, composite, calza blanca/gris, amalgama en buen estado, empaste, obturado, incrustación, inlay, onlay, overlay, restauración adaptada/íntegra/funcional -> "Restaurado".
                   - Obturación temporal, curación temporal/provisional, cemento temporal, ionómero de vidrio, IRM, Cavit, coltosol, eugenol, pasta provisional -> "Obturación Temporal".
                   [TRATAMIENTOS INTERNOS Y ESTRUCTURALES (Todo el diente -> estadoGeneral)]
                   - Endodoncia, tratamiento de conducto(s), conductos obturados, nervio sacado, desvitalizado, pulpectomía, pulpotomía -> "Endodoncia".
                   - Núcleo, poste, perno, espigo, muñón, núcleo colado, poste de fibra de vidrio -> "Núcleo/Poste".
                   [PRÓTESIS (Todo el diente -> estadoGeneral)]
                   - Corona, funda, corona completa, prótesis fija individual, corona de zirconio, metal-porcelana, disilicato, porcelana, jacket -> "Corona".
                   - Póntico, diente falso en puente, tramo de puente fijo, pieza suspendida -> "Póntico".
                   - Prótesis removible, PPR, placa, dientes de quitar y poner, esquelético, prótesis acrílica/flexible/total -> "Prótesis Removible".
                   - Implante, implante dental/osteointegrado, tornillo de titanio -> "Implante".
                   [AUSENCIA O POSICIÓN (Todo el diente -> estadoGeneral)]
                   - Ausente, ausente clínico, perdido, falta, no está, agenesia, anodoncia -> "Ausente".
                   - Extraído, exodoncia, extracción confirmada, exodonciado, sacado -> "Extraído".
                   - Resto radicular, raíz retenida/podrida, corona destruida, muñón radicular sin corona -> "Resto Radicular".
                   - Retenido, incluido, impactado en hueso, no erupcionado, sumergido, anquilosado -> "Retenido".
                   - En erupción, erupción parcial, semi-erupcionado, erupcionando, asomando -> "En Erupción".
                Formato requerido: {"dientes":[{"numero":14,"estadoGeneral":"Presente","caras":{"mesial":"Caries","distal":"Sano","vestibular":"Sano","palatino_lingual":"Sano","oclusal_incisal":"Restaurado"}},{"numero":15,"estadoGeneral":"Rest. Defectuosa"}]}
                """.formatted(EstadoClinico.valoresPermitidosComoTexto());
    }
}
