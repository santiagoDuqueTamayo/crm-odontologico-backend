package com.crm.odontologico.kassar.odontograma.domain.model;

/**
 * Representa la arcada dental según el cuadrante FDI y define el nombre
 * anatómico correcto para la cara interna del diente.
 * <ul>
 *   <li>Cuadrantes 1 y 2 (Superiores): la cara interna mira hacia el paladar (<b>Palatino</b>).</li>
 *   <li>Cuadrantes 3 y 4 (Inferiores): la cara interna mira hacia la lengua (<b>Lingual</b>).</li>
 * </ul>
 */
public enum ArcadaDental {

    SUPERIOR("Palatino"),
    INFERIOR("Lingual");

    private final String etiquetaCaraInterna;

    ArcadaDental(String etiquetaCaraInterna) {
        this.etiquetaCaraInterna = etiquetaCaraInterna;
    }

    public String getEtiquetaCaraInterna() {
        return etiquetaCaraInterna;
    }

    public static ArcadaDental porCuadrante(int cuadrante) {
        return switch (cuadrante) {
            case 1, 2 -> SUPERIOR;
            case 3, 4 -> INFERIOR;
            default -> throw new IllegalArgumentException("Cuadrante FDI inválido: " + cuadrante);
        };
    }
}
