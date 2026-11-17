package com.crm.odontologico.kassar.odontograma.domain.model;

import com.fasterxml.jackson.annotation.JsonValue;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Catálogo cerrado de estados clínicos permitidos por el contrato con el Frontend,
 * incluyendo su color hexadecimal semántico oficial, si aplica al diente/raíz general
 * y si afecta exclusivamente a la raíz sin alterar las 5 caras coronales.
 */
public enum EstadoClinico {

    // --- ESTADOS DE SALUD Y PREVENCIÓN ---
    SANO("Sano", "#22C55E", false, false),                      // green-500
    PRESENTE("Presente", null, false, false),                   // Estado base/neutro
    SELLANTE("Sellante", "#06B6D4", false, false),              // cyan-500

    // --- PATOLOGÍAS Y DAÑOS CORONALES (Por caras y parte baja del diente, NO raíz) ---
    CARIES("Caries", "#EF4444", false, false),                  // red-500
    FRACTURADO("Fracturado", "#F97316", false, false),          // orange-500
    DESGASTE("Desgaste/Abfracción", "#FDBA74", false, false),   // orange-300 (Bruxismo, cepillado agresivo)
    DEFECTUOSO("Rest. Defectuosa", "#E11D48", false, false),    // rose-600 (Resinas o amalgamas filtradas/para cambio)

    // --- TRATAMIENTOS RESTAURATIVOS (Por caras y parte baja del diente, NO raíz) ---
    RESTAURADO("Restaurado", "#3B82F6", false, false),          // blue-500 (Resina, Amalgama en buen estado)
    TEMPORAL("Obturación Temporal", "#94A3B8", false, false),   // slate-400 (Ionómeros, curaciones provisionales)

    // --- TRATAMIENTOS ENDODÓNTICOS Y ESTRUCTURALES (Diente completo: Raíz + Corona + Caras) ---
    ENDODONCIA("Endodoncia", "#D946EF", true, false),           // fuchsia-500 (Tratamiento de conducto)
    NUCLEO("Núcleo/Poste", "#8B5CF6", true, false),             // violet-500 (Perno/poste intrarradicular)

    // --- LESIONES Y TRATAMIENTOS EXCLUSIVOS DE LA RAÍZ (Solo pintan la raíz, NO corona ni caras) ---
    APICECTOMIA("Apicectomía", "#C026D3", true, true),                  // fuchsia-600 (Cirugía apical / obturación retrógrada)
    LESION_PERIAPICAL("Lesión Periapical", "#991B1B", true, true),      // red-800 (Foco infeccioso, absceso/quiste/granuloma apical)
    FRACTURA_RADICULAR("Fractura Radicular", "#B91C1C", true, true),    // red-700 (Fractura vertical/horizontal/corono-radicular)
    PERFORACION_RADICULAR("Perforación Radicular", "#BE123C", true, true), // rose-700 (Falsa vía, perforación tercio medio/apical)
    LESION_FURCA("Lesión de Furca", "#B45309", true, true),             // amber-700 (Compromiso/involucración de furca grado 1-3)
    RECESION_GINGIVAL("Recesión Gingival", "#D97706", true, true),      // amber-600 (Cuellos destapados, raíces expuestas)
    CALCULO_SUBGINGIVAL("Cálculo Subgingival", "#92400E", true, true),  // amber-800 (Piedras debajo de la encía / sarro subgingival)
    REABSORCION_RADICULAR("Reabsorción Radicular", "#7C2D12", true, true), // orange-900 (Reabsorción externa/interna, raíces enanas)
    ANOMALIA_RADICULAR("Anomalía Radicular", "#0369A1", true, true),    // sky-700 (Dilaceración, raíz curva, supernumeraria, concrescencia)
    RESTO_RADICULAR("Resto Radicular", "#78716C", true, true),          // stone-500 (Solo queda la raíz sin corona)

    // --- PRÓTESIS (Corona completa y caras) ---
    CORONA("Corona", "#EAB308", true, false),                   // yellow-500
    PONTICO("Póntico", "#FBBF24", true, false),                 // amber-400
    PROTESIS_REMOVIBLE("Prótesis Removible", "#F472B6", true, false), // pink-400
    IMPLANTE("Implante", "#14B8A6", true, false),               // teal-500

    // --- ESTADOS DE AUSENCIA O POSICIÓN (Todo el diente) ---
    AUSENTE("Ausente", "#6B7280", true, false),                 // gray-500
    EXTRAIDO("Extraído", "#64748B", true, false),               // slate-500
    RETENIDO("Retenido", "#A855F7", true, false),               // purple-500
    EN_ERUPCION("En Erupción", "#84CC16", true, false);         // lime-500

    private final String etiqueta;
    private final String colorHex;
    private final boolean inhabilitaCarasIndividuales;
    private final boolean exclusivoDeRaiz;

    EstadoClinico(
            String etiqueta,
            String colorHex,
            boolean inhabilitaCarasIndividuales,
            boolean exclusivoDeRaiz
    ) {
        this.etiqueta = etiqueta;
        this.colorHex = colorHex;
        this.inhabilitaCarasIndividuales = inhabilitaCarasIndividuales;
        this.exclusivoDeRaiz = exclusivoDeRaiz;
    }

    @JsonValue
    public String getEtiqueta() {
        return etiqueta;
    }

    public String getColorHex() {
        return colorHex;
    }

    /**
     * Indica si el estado se asigna a nivel general del diente/raíz ({@code estadoGeneral}).
     */
    public boolean isInhabilitaCarasIndividuales() {
        return inhabilitaCarasIndividuales;
    }

    /**
     * Indica si el estado afecta exclusivamente a la raíz del diente, por lo que las 5 caras
     * coronales deben mantenerse en {@link #SANO} (salvo que tengan sus propios hallazgos coronales).
     */
    public boolean isExclusivoDeRaiz() {
        return exclusivoDeRaiz;
    }

    public static Optional<EstadoClinico> fromTexto(String valor) {
        if (valor == null || valor.isBlank()) {
            return Optional.empty();
        }
        String normalizado = normalizar(valor);

        return switch (normalizado) {
            // Salud y prevención
            case "sano", "intacto", "cero", "0", "codigo 0", "sin patologia", "normal" -> Optional.of(SANO);
            case "presente" -> Optional.of(PRESENTE);
            case "sellante", "sellador", "sellante de fosas y fisuras", "sellante preventivo" -> Optional.of(SELLANTE);

            // Patologías y daños coronales
            case "caries", "lesion cariosa", "lesion inicial", "mancha blanca", "mancha marron", "descalcificacion",
                 "caries incipiente", "icdas 1", "icdas 2", "icdas 3", "icdas 4", "icdas 5", "icdas 6",
                 "icdas", "caries microcavitada", "ruptura de esmalte", "caries dentinaria",
                 "caries cavitada", "caries abierta", "hueco", "lesion activa", "caries profunda",
                 "caries recidivante", "caries secundaria", "caribe" -> Optional.of(CARIES);

            case "fracturado", "fractura", "fractura coronal", "diente roto", "roto", "rota",
                 "fractura de esmalte", "fractura de dentina", "fractura incisal", "astillado",
                 "fisura", "fisura dental" -> Optional.of(FRACTURADO);

            case "desgaste/abfraccion", "desgaste", "abfraccion", "lesion cervical no cariosa", "lcnc",
                 "abrasion", "desgaste por cepillado", "cepillado agresivo", "raya cervical", "atricion",
                 "dientes desgastados", "desgastado", "desgastada", "bruxismo", "erosion",
                 "desgaste quimico", "disolucion del esmalte", "faceta de desgaste" -> Optional.of(DESGASTE);

            case "rest. defectuosa", "restauracion defectuosa", "defectuoso", "defectuosa",
                 "filtrada", "filtrado", "microfiltrada", "microfiltrado", "resina filtrada",
                 "amalgama filtrada", "tincion marginal", "pigmentada", "pigmentado",
                 "desadaptada", "desadaptado", "margenes abiertos", "sobrecontorneada",
                 "sobrecontorneado", "amalgama fracturada", "resina fracturada",
                 "restauracion rota", "restauracion caida", "desalojada", "desalojado",
                 "para cambio" -> Optional.of(DEFECTUOSO);

            // Tratamientos restaurativos por caras
            case "restaurado", "resina", "resina compuesta", "composite", "calza", "calza blanca",
                 "restauracion estetica", "amalgama", "calza gris", "empaste metalico", "empaste",
                 "plata", "obturado", "obturacion", "incrustacion", "inlay", "onlay", "overlay",
                 "adaptada", "adaptado", "en buen estado", "funcional", "integra", "integro" -> Optional.of(RESTAURADO);

            case "obturacion temporal", "temporal", "curacion temporal", "curacion provisional",
                 "provisional", "cemento temporal", "ionomero", "ionomero de vidrio",
                 "irm", "cavit", "coltosol", "eugenol", "oxido de zinc", "pasta temporal", "base" -> Optional.of(TEMPORAL);

            // 1. Grupo Endodóntico
            case "endodoncia", "endodoncia terminada", "tratamiento de conducto", "tratamiento de conductos",
                 "nervio sacado", "conductos obturados", "pulpotomia", "pulpectomia",
                 "tratamiento de nervio", "desvitalizado", "terapia pulpar" -> Optional.of(ENDODONCIA);

            case "nucleo/poste", "nucleo", "poste", "perno", "tiene un perno", "espigo", "munon",
                 "nucleo colado", "nucleo colado adaptado", "poste de fibra", "poste de fibra de vidrio",
                 "perno intrarradicular", "soporte intrarradicular" -> Optional.of(NUCLEO);

            case "apicectomia", "tiene apicectomia", "obturacion retrograda", "cirugia apical",
                 "cirugia de la raiz", "reseccion apical" -> Optional.of(APICECTOMIA);

            // 2. Grupo Infeccioso Periapical
            case "lesion periapical", "lesion apical", "se observa lesion apical", "foco infeccioso",
                 "tiene un foco infeccioso", "imagen radiolucida en el apice", "imagen radiolucida apical",
                 "sombra apical", "sombra en la raiz", "absceso periapical", "absceso apical",
                 "granuloma apical", "quiste periapical", "quiste radicular", "periodontitis apical" -> Optional.of(LESION_PERIAPICAL);

            // 3. Grupo de Fracturas y Accidentes Radiculares
            case "fractura radicular", "fractura de raiz", "fractura vertical", "tiene fractura vertical",
                 "fractura horizontal radicular", "fractura corono-radicular", "fractura coronorradicular",
                 "diente partido hasta la raiz", "raiz fracturada", "raiz partida" -> Optional.of(FRACTURA_RADICULAR);

            case "perforacion radicular", "perforacion de raiz", "perforacion en el tercio medio",
                 "perforacion apical", "perforacion furcal", "falsa via", "hicieron una falsa via" -> Optional.of(PERFORACION_RADICULAR);

            // 4. Grupo Periodontal
            case "lesion de furca", "furca", "furca grado 1", "furca grado 2", "furca grado 3",
                 "furca grado 2 por vestibular", "involucracion de furca", "compromiso de furca" -> Optional.of(LESION_FURCA);

            case "recesion gingival", "recesion gingival severa", "exposicion radicular",
                 "raices expuestas", "raiz expuesta", "cuellos destapados", "cuello destapado",
                 "tiene los cuellos destapados", "retraccion gingival" -> Optional.of(RECESION_GINGIVAL);

            case "calculo subgingival", "calculo subgingival abundante", "sarro subgingival",
                 "piedras debajo de la encia", "tartaro subgingival" -> Optional.of(CALCULO_SUBGINGIVAL);

            // 5. Grupo de Reabsorciones
            case "reabsorcion radicular", "reabsorcion externa", "tiene reabsorcion externa",
                 "reabsorcion interna", "reabsorcion cervical", "raices enanas", "raiz enana",
                 "raiz corta", "rizolisis" -> Optional.of(REABSORCION_RADICULAR);

            // 6. Grupo Anatómico Radicular
            case "anomalia radicular", "dilaceracion", "dilaceracion radicular", "raiz dilacerada",
                 "raiz curva", "tiene raiz curva", "apice en gancho", "raiz supernumeraria",
                 "tiene tres raices", "raiz extra", "concrescencia", "fusion radicular",
                 "raices fusionadas", "raices unidas" -> Optional.of(ANOMALIA_RADICULAR);

            // Prótesis
            case "corona", "corona completa", "funda", "protesis fija", "protesis fija individual",
                 "zirconio", "corona de zirconio", "metal-porcelana", "porcelana",
                 "disilicato", "disilicato de litio", "ceramica", "jacket" -> Optional.of(CORONA);

            case "pontico", "diente pontico", "pieza de puente", "puente fijo",
                 "tramo de puente", "diente falso", "diente suspendido" -> Optional.of(PONTICO);

            case "protesis removible", "removible", "ppr", "protesis parcial removible",
                 "protesis total", "placa", "dientes de quitar y poner", "esqueletico",
                 "protesis acrilica", "protesis flexible" -> Optional.of(PROTESIS_REMOVIBLE);

            case "implante", "implante dental", "implante osteointegrado",
                 "tornillo de titanio", "fijacion implantologica" -> Optional.of(IMPLANTE);

            // Ausencia o posición
            case "ausente", "ausente clinico", "perdido", "falta", "agenesia",
                 "ausencia congenita", "anodoncia" -> Optional.of(AUSENTE);

            case "extraccion", "extraido", "exodoncia", "exodonciado", "sacado" -> Optional.of(EXTRAIDO);

            case "resto radicular", "raiz retenida", "raiz podrida", "corona destruida",
                 "diente remanente", "remanente radicular", "munon radicular" -> Optional.of(RESTO_RADICULAR);

            case "retenido", "no erupcionado", "incluido", "impactado",
                 "retenido en hueso", "sumergido", "anquilosado" -> Optional.of(RETENIDO);

            case "en erupcion", "erupcion parcial", "semi-erupcionado", "semierupcionado",
                 "en proceso de erupcion", "erupcionando", "asomando" -> Optional.of(EN_ERUPCION);

            default -> Arrays.stream(values())
                    .filter(estado -> normalizar(estado.etiqueta).equals(normalizado)
                            || normalizar(estado.name()).equals(normalizado))
                    .findFirst();
        };
    }

    public static String valoresPermitidosComoTexto() {
        return Arrays.stream(values())
                .map(EstadoClinico::getEtiqueta)
                .collect(Collectors.joining(", "));
    }

    private static String normalizar(String input) {
        String sinTildes = Normalizer.normalize(input.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT);
    }
}
