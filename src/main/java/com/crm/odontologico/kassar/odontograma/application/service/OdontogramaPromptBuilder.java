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
                   - Si es una lesión RADICULAR, ENDODÓNTICA, PROTÉSICA o de DIENTE COMPLETO (Endodoncia, Núcleo/Poste, Apicectomía, Lesión Periapical, Fractura Radicular, Perforación Radicular, Lesión de Furca, Recesión Gingival, Cálculo Subgingival, Reabsorción Radicular, Anomalía Radicular, Resto Radicular, Corona, Póntico, Prótesis Removible, Implante, Ausente, Extraído, Retenido, En Erupción, o un estado sin cara específica), asígnalo directamente en "estadoGeneral".
                   - Si el hallazgo afecta caras específicas de la corona (y no hay estado radicular/general), pon "estadoGeneral": "Presente" y en "caras" asigna el estado a las caras afectadas y "Sano" a las demás.
                   - Si un mismo diente tiene TANTO una afección de raíz como hallazgos en caras coronales, pon la afección de raíz en "estadoGeneral" y las caras afectadas en "caras".
                4. Mapa FDI permitido: 11-18, 21-28, 31-38, 41-48 (cuadrantes: 1=sup der, 2=sup izq, 3=inf izq, 4=inf der; posiciones: 1=incisivo central/paleta, 2=lateral, 3=canino/colmillo, 4=1er premolar, 5=2do premolar, 6=1er molar, 7=2do molar, 8=3er molar/cordal).
                5. Caras (5 llaves): "mesial", "distal", "vestibular" (labial/bucal/cervical), "palatino_lingual" (palatina/lingual), "oclusal_incisal" (oclusal/incisal). Compuestos: ocluso-mesial=oclusal_incisal+mesial; ocluso-distal=oclusal_incisal+distal; MOD=mesial+oclusal_incisal+distal.
                6. Estados permitidos exactos: [%s].
                7. Agrupación de sinónimos clínicos hacia el estado oficial:
                   [SALUD Y PREVENCIÓN]
                   - Sano, intacto, cero, código 0, sin patología, normal -> "Sano".
                   - Presente (estado general neutro cuando solo hay hallazgos por caras) -> "Presente".
                   - Sellante, sellador, sellante de fosas y fisuras -> "Sellante".
                   [PATOLOGÍAS Y TRATAMIENTOS CORONALES (Por caras)]
                   - Caries, lesión cariosa, mancha blanca/marrón, descalcificación, ICDAS 1-6, caries microcavitada/dentinaria/cavitada/abierta/profunda/recidivante/secundaria, hueco, caribe -> "Caries".
                   - Fracturado, fractura coronal/incisal/esmalte/dentina, diente roto, astillado, fisura dental -> "Fracturado".
                   - Desgaste, abfracción, bruxismo, atrición, abrasión, erosión, cepillado agresivo, lesión cervical no cariosa (LCNC), raya cervical, faceta de desgaste -> "Desgaste/Abfracción".
                   - Restauración defectuosa, resina/amalgama filtrada o microfiltrada, amalgama/resina fracturada o rota, restauración caída/desalojada, desadaptada, márgenes abiertos, sobrecontorneada, para cambio -> "Rest. Defectuosa".
                   - Restaurado, resina, composite, calza blanca/gris, amalgama en buen estado, empaste, obturado, incrustación, inlay, onlay, overlay -> "Restaurado".
                   - Obturación temporal, curación temporal/provisional, cemento temporal, ionómero de vidrio, IRM, Cavit, coltosol, eugenol -> "Obturación Temporal".
                   [1. GRUPO ENDODÓNTICO (Raíz / Diente completo -> estadoGeneral)]
                   - Endodoncia, endodoncia terminada, tratamiento de conducto(s), conductos obturados, nervio sacado, desvitalizado, pulpectomía, pulpotomía -> "Endodoncia".
                   - Núcleo, poste, perno, tiene un perno, espigo, muñón, núcleo colado adaptado, poste de fibra de vidrio -> "Núcleo/Poste".
                   - Apicectomía, tiene apicectomía, obturación retrógrada, cirugía apical, cirugía de la raíz -> "Apicectomía".
                   [2. GRUPO INFECCIOSO PERIAPICAL (Solo Raíz -> estadoGeneral)]
                   - Lesión periapical, lesión apical, se observa lesión apical, foco infeccioso, imagen radiolúcida en el ápice, sombra apical, absceso periapical, quiste/granuloma apical -> "Lesión Periapical".
                   [3. GRUPO FRACTURAS Y ACCIDENTES RADICULARES (Solo Raíz -> estadoGeneral)]
                   - Fractura radicular, fractura vertical, fractura horizontal de raíz, fractura corono-radicular, diente partido hasta la raíz -> "Fractura Radicular".
                   - Perforación radicular, perforación en tercio medio/apical, falsa vía, hicieron una falsa vía -> "Perforación Radicular".
                   [4. GRUPO PERIODONTAL (Solo Raíz -> estadoGeneral)]
                   - Lesión de furca, furca grado 1/2/3, furca por vestibular, involucración de furca, compromiso de furca -> "Lesión de Furca".
                   - Recesión gingival, recesión gingival severa, exposición radicular, raíces expuestas, tiene los cuellos destapados -> "Recesión Gingival".
                   - Cálculo subgingival, cálculo subgingival abundante, sarro subgingival, piedras debajo de la encía -> "Cálculo Subgingival".
                   [5. GRUPO DE REABSORCIONES (Solo Raíz -> estadoGeneral)]
                   - Reabsorción radicular, reabsorción externa/interna/cervical, raíces enanas, raíz corta -> "Reabsorción Radicular".
                   [6. GRUPO ANATÓMICO RADICULAR (Solo Raíz -> estadoGeneral)]
                   - Dilaceración, raíz dilacerada, raíz curva, ápice en gancho, raíz supernumeraria, tiene tres raíces, raíz extra, concrescencia, raíces fusionadas, raíces unidas -> "Anomalía Radicular".
                   [PRÓTESIS Y AUSENCIA (estadoGeneral)]
                   - Corona, funda, prótesis fija individual, corona de zirconio, metal-porcelana, disilicato -> "Corona".
                   - Póntico, diente falso en puente, tramo de puente fijo -> "Póntico".
                   - Prótesis removible, PPR, placa, dientes de quitar y poner, esquelético -> "Prótesis Removible".
                   - Implante, implante dental, tornillo de titanio -> "Implante".
                   - Ausente, perdido, falta, agenesia -> "Ausente".
                   - Extraído, exodoncia, extracción, exodonciado -> "Extraído".
                   - Resto radicular, raíz retenida/podrida, corona destruida -> "Resto Radicular".
                   - Retenido, incluido, impactado en hueso, no erupcionado -> "Retenido".
                   - En erupción, erupción parcial, semi-erupcionado, erupcionando -> "En Erupción".
                Formato requerido: {"dientes":[{"numero":14,"estadoGeneral":"Presente","caras":{"mesial":"Caries","distal":"Sano","vestibular":"Sano","palatino_lingual":"Sano","oclusal_incisal":"Restaurado"}},{"numero":21,"estadoGeneral":"Fractura Radicular"}]}
                """.formatted(EstadoClinico.valoresPermitidosComoTexto());
    }
}
