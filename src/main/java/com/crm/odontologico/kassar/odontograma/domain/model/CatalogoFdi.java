package com.crm.odontologico.kassar.odontograma.domain.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Catálogo autoritativo de los 32 dientes permanentes según la nomenclatura internacional FDI
 * (Federación Dental Internacional: cuadrantes 1 al 4, posiciones 1 al 8).
 */
public final class CatalogoFdi {

    public static final String ETIQUETA_MESIAL = "Mesial";
    public static final String ETIQUETA_DISTAL = "Distal";
    public static final String ETIQUETA_VESTIBULAR = "Vestibular";

    private static final Map<Integer, DienteFdiMetadata> CATALOGO;

    static {
        Map<Integer, DienteFdiMetadata> map = new LinkedHashMap<>();
        for (int cuadrante = 1; cuadrante <= 4; cuadrante++) {
            for (int posicion = 1; posicion <= 8; posicion++) {
                int codigoFdi = (cuadrante * 10) + posicion;
                ArcadaDental arcada = ArcadaDental.porCuadrante(cuadrante);
                GrupoDental grupo = GrupoDental.porPosicionEnCuadrante(posicion);
                String nombreColoquial = construirNombreColoquial(cuadrante, posicion);

                map.put(codigoFdi, new DienteFdiMetadata(
                        codigoFdi,
                        cuadrante,
                        posicion,
                        nombreColoquial,
                        arcada,
                        grupo
                ));
            }
        }
        CATALOGO = Collections.unmodifiableMap(map);
    }

    private CatalogoFdi() {
        // Utility class
    }

    public static boolean esFdiValido(int numero) {
        return CATALOGO.containsKey(numero);
    }

    public static Optional<DienteFdiMetadata> buscarPorNumero(int numero) {
        return Optional.ofNullable(CATALOGO.get(numero));
    }

    public static DienteFdiMetadata obtenerPorNumero(int numero) {
        DienteFdiMetadata metadata = CATALOGO.get(numero);
        if (metadata == null) {
            throw new IllegalArgumentException(
                    "El número de diente " + numero + " no pertenece al estándar FDI de 32 dientes (11-18, 21-28, 31-38, 41-48).");
        }
        return metadata;
    }

    public static Set<Integer> codigosPermitidos() {
        return CATALOGO.keySet();
    }

    public static Map<Integer, DienteFdiMetadata> obtenerTodos() {
        return CATALOGO;
    }

    private static String construirNombreColoquial(int cuadrante, int posicion) {
        String tipoDiente = switch (posicion) {
            case 1 -> "Incisivo central";
            case 2 -> "Incisivo lateral";
            case 3 -> "Canino";
            case 4 -> "Primer premolar";
            case 5 -> "Segundo premolar";
            case 6 -> "Primer molar";
            case 7 -> "Segundo molar";
            case 8 -> "Tercer molar (cordal)";
            default -> throw new IllegalArgumentException("Posición inválida: " + posicion);
        };

        String ubicacionCuadrante = switch (cuadrante) {
            case 1 -> "superior derecho";
            case 2 -> "superior izquierdo";
            case 3 -> "inferior izquierdo";
            case 4 -> "inferior derecho";
            default -> throw new IllegalArgumentException("Cuadrante inválido: " + cuadrante);
        };

        return tipoDiente + " " + ubicacionCuadrante;
    }

    /**
     * Metadatos anatómicos inmutables de un diente dentro de la nomenclatura FDI.
     */
    public record DienteFdiMetadata(
            int numero,
            int cuadrante,
            int posicion,
            String nombreColoquial,
            ArcadaDental arcada,
            GrupoDental grupo
    ) {
        public String etiquetaCaraInterna() {
            return arcada.getEtiquetaCaraInterna();
        }

        public String etiquetaCaraMasticatoria() {
            return grupo.getEtiquetaCaraMasticatoria();
        }

        public boolean esAnterior() {
            return grupo == GrupoDental.ANTERIOR;
        }

        public boolean esPosterior() {
            return grupo == GrupoDental.POSTERIOR;
        }
    }
}
