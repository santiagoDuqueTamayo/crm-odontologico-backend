package com.crm.odontologico.kassar.odontograma.domain.model;

import com.fasterxml.jackson.annotation.JsonValue;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Catálogo cerrado de estados clínicos permitidos por el contrato con el Frontend,
 * incluyendo su color hexadecimal semántico oficial y reglas clínicas.
 */
public enum EstadoClinico {

    SANO("Sano", "#22C55E", false),
    CARIES("Caries", "#EF4444", false),
    RESTAURADO("Restaurado", "#3B82F6", false),
    FRACTURADO("Fracturado", "#F97316", false),
    AUSENTE("Ausente", "#6B7280", true),
    EXTRAIDO("Extraído", "#64748B", true),
    RETENIDO("Retenido", "#A855F7", true),
    CORONA("Corona", "#EAB308", false),
    IMPLANTE("Implante", "#14B8A6", true),
    SELLANTE("Sellante", "#06B6D4", false),
    PRESENTE("Presente", null, false);

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
     * tengan patologías individuales (ej. Ausente, Extraído, Retenido, Implante).
     */
    public boolean isInhabilitaCarasIndividuales() {
        return inhabilitaCarasIndividuales;
    }

    /**
     * Busca un EstadoClinico de manera insensible a mayúsculas, minúsculas y tildes,
     * soportando además sinónimos clínicos habituales del modelo local (ej. Resina/Amalgama -> Restaurado,
     * Extraccion -> Extraído).
     */
    public static Optional<EstadoClinico> fromTexto(String valor) {
        if (valor == null || valor.isBlank()) {
            return Optional.empty();
        }
        String normalizado = normalizar(valor);

        return switch (normalizado) {
            case "resina", "amalgama", "obturado", "obturacion" -> Optional.of(RESTAURADO);
            case "extraccion", "extraido", "exodoncia" -> Optional.of(EXTRAIDO);
            case "fractura" -> Optional.of(FRACTURADO);
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
