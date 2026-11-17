package com.crm.odontologico.kassar.odontograma.domain.model;

import com.fasterxml.jackson.annotation.JsonValue;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Catálogo cerrado de estados clínicos permitidos por el contrato con el Frontend,
 * incluyendo su color hexadecimal semántico oficial y si aplica al diente completo.
 */
public enum EstadoClinico {

    // --- ESTADOS DE SALUD Y PREVENCIÓN ---
    SANO("Sano", "#22C55E", false),                      // green-500
    PRESENTE("Presente", null, false),                   // Estado base/neutro
    SELLANTE("Sellante", "#06B6D4", false),              // cyan-500

    // --- PATOLOGÍAS Y DAÑOS ---
    CARIES("Caries", "#EF4444", false),                  // red-500
    FRACTURADO("Fracturado", "#F97316", false),          // orange-500
    DESGASTE("Desgaste/Abfracción", "#FDBA74", false),   // orange-300 (Bruxismo, cepillado agresivo)
    DEFECTUOSO("Rest. Defectuosa", "#E11D48", false),    // rose-600 (Resinas o amalgamas filtradas/para cambio)

    // --- TRATAMIENTOS RESTAURATIVOS (Por caras) ---
    RESTAURADO("Restaurado", "#3B82F6", false),          // blue-500 (Resina, Amalgama en buen estado)
    TEMPORAL("Obturación Temporal", "#94A3B8", false),   // slate-400 (Ionómeros, curaciones provisionales)

    // --- TRATAMIENTOS INTERNOS Y ESTRUCTURALES (Todo el diente) ---
    ENDODONCIA("Endodoncia", "#D946EF", true),           // fuchsia-500 (Tratamiento de conducto en la raíz)
    NUCLEO("Núcleo/Poste", "#8B5CF6", true),             // violet-500 (Perno dentro de la raíz para sostener corona)

    // --- PRÓTESIS (Todo el diente) ---
    CORONA("Corona", "#EAB308", true),                   // yellow-500 (Envuelve todas las caras del diente)
    PONTICO("Póntico", "#FBBF24", true),                 // amber-400 (Diente falso suspendido en un puente)
    PROTESIS_REMOVIBLE("Prótesis Removible", "#F472B6", true), // pink-400 (Dientes de "quitar y poner")
    IMPLANTE("Implante", "#14B8A6", true),               // teal-500 (Tornillo de titanio en el hueso)

    // --- ESTADOS DE AUSENCIA O POSICIÓN (Todo el diente) ---
    AUSENTE("Ausente", "#6B7280", true),                 // gray-500 (Ausencia clínica o congénita)
    EXTRAIDO("Extraído", "#64748B", true),               // slate-500 (Exodoncia clínica confirmada)
    RESTO_RADICULAR("Resto Radicular", "#78716C", true), // stone-500 (Solo queda la raíz sin corona)
    RETENIDO("Retenido", "#A855F7", true),               // purple-500 (Cordales o dientes impactados en hueso)
    EN_ERUPCION("En Erupción", "#84CC16", true);         // lime-500 (Erupción parcial o en proceso)

    private final String etiqueta;
    private final String colorHex;
    private final boolean inhabilitaCarasIndividuales;

    EstadoClinico(String etiqueta, String colorHex, boolean inhabilitaCarasIndividuales) {
        this.etiqueta = etiqueta;
        this.colorHex = colorHex;
        this.inhabilitaCarasIndividuales = inhabilitaCarasIndividuales;
    }

    @JsonValue
    public String getEtiqueta() {
        return etiqueta;
    }

    public String getColorHex() {
        return colorHex;
    }

    /**
     * Indica si el estado aplica únicamente al diente completo e impide que sus 5 caras
     * tengan patologías individuales (ej. Corona, Endodoncia, Ausente, Extraído, Implante).
     */
    public boolean isInhabilitaCarasIndividuales() {
        return inhabilitaCarasIndividuales;
    }

    /**
     * Busca un {@link EstadoClinico} de manera insensible a mayúsculas, minúsculas y tildes,
     * soportando tanto las etiquetas oficiales como el nombre de la constante y sus sinónimos clínicos.
     */
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

            // Patologías y daños
            case "caries", "lesion cariosa", "lesion inicial", "mancha blanca", "mancha marron", "descalcificacion",
                 "caries incipiente", "icdas 1", "icdas 2", "icdas 3", "icdas 4", "icdas 5", "icdas 6",
                 "icdas", "caries microcavitada", "ruptura de esmalte", "caries dentinaria",
                 "caries cavitada", "caries abierta", "hueco", "lesion activa", "caries profunda",
                 "caries recidivante", "caries secundaria", "caribe" -> Optional.of(CARIES);

            case "fracturado", "fractura", "fractura coronal", "diente roto", "roto", "rota",
                 "fractura de esmalte", "fractura de dentina", "fractura incisal", "astillado",
                 "fisura", "fisura dental" -> Optional.of(FRACTURADO);

            case "desgaste/abfraccion", "desgaste", "abfraccion", "cuello destapado",
                 "lesion cervical no cariosa", "lcnc", "abrasion", "desgaste por cepillado",
                 "cepillado agresivo", "raya cervical", "atricion", "dientes desgastados",
                 "desgastado", "desgastada", "bruxismo", "erosion", "desgaste quimico",
                 "disolucion del esmalte", "faceta de desgaste" -> Optional.of(DESGASTE);

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

            // Tratamientos internos y estructurales (todo el diente)
            case "endodoncia", "tratamiento de conducto", "tratamiento de conductos",
                 "nervio sacado", "conductos obturados", "pulpotomia", "pulpectomia",
                 "tratamiento de nervio", "desvitalizado", "terapia pulpar" -> Optional.of(ENDODONCIA);

            case "nucleo/poste", "nucleo", "poste", "perno", "espigo", "munon",
                 "nucleo colado", "poste de fibra", "poste de fibra de vidrio",
                 "perno intrarradicular" -> Optional.of(NUCLEO);

            // Prótesis (todo el diente)
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

            // Ausencia o posición (todo el diente)
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
