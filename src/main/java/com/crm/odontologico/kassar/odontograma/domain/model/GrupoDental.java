package com.crm.odontologico.kassar.odontograma.domain.model;

/**
 * Clasifica el diente según su morfología funcional y determina el nombre
 * anatómico de su superficie de corte o trituración.
 * <ul>
 *   <li><b>ANTERIOR</b> (posiciones 1, 2 y 3: Incisivos y Caninos): poseen borde <b>Incisal</b>.</li>
 *   <li><b>POSTERIOR</b> (posiciones 4 a 8: Premolares y Molares): poseen cara <b>Oclusal</b>.</li>
 * </ul>
 */
public enum GrupoDental {

    ANTERIOR("Incisal"),
    POSTERIOR("Oclusal");

    private final String etiquetaCaraMasticatoria;

    GrupoDental(String etiquetaCaraMasticatoria) {
        this.etiquetaCaraMasticatoria = etiquetaCaraMasticatoria;
    }

    public String getEtiquetaCaraMasticatoria() {
        return etiquetaCaraMasticatoria;
    }

    public static GrupoDental porPosicionEnCuadrante(int posicion) {
        if (posicion >= 1 && posicion <= 3) {
            return ANTERIOR;
        }
        if (posicion >= 4 && posicion <= 8) {
            return POSTERIOR;
        }
        throw new IllegalArgumentException("Posición dental FDI inválida: " + posicion);
    }
}
