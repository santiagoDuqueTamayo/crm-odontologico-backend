package com.crm.odontologico.kassar.odontograma.application.validation;

import com.crm.odontologico.kassar.odontograma.domain.model.CatalogoFdi;
import com.crm.odontologico.kassar.odontograma.domain.model.CatalogoFdi.DienteFdiMetadata;
import com.crm.odontologico.kassar.odontograma.domain.model.EstadoClinico;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Valida estructural y clínicamente el JSON parcial devuelto por el modelo de IA y
 * aplica autocorrección anatómica utilizando los metadatos de {@link CatalogoFdi}.
 */
@Component
public class AiResponseValidator {

    private static final List<String> LLAVES_MESIAL = List.of("mesial");
    private static final List<String> LLAVES_DISTAL = List.of("distal");
    private static final List<String> LLAVES_VESTIBULAR = List.of("vestibular", "labial", "bucal", "cervical");
    private static final List<String> LLAVES_CARA_INTERNA = List.of("palatino_lingual", "palatino", "palatina", "lingual");
    private static final List<String> LLAVES_CARA_MASTICATORIA = List.of("oclusal_incisal", "oclusal", "incisal");

    private final ObjectMapper objectMapper;

    public AiResponseValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ValidationResult validar(String rawAiResponse) {
        if (rawAiResponse == null || rawAiResponse.isBlank()) {
            return ValidationResult.fallido(List.of(
                    "La respuesta del modelo está vacía. Debes responder exclusivamente con un objeto JSON válido."));
        }

        JsonNode root = parsearJson(rawAiResponse);
        if (root == null) {
            return ValidationResult.fallido(List.of(
                    "El texto devuelto no es un JSON válido. Devuelve únicamente un objeto JSON sin texto adicional."));
        }

        if (!root.isObject()) {
            return ValidationResult.fallido(List.of(
                    "La raíz de la respuesta debe ser un objeto JSON con la propiedad 'dientes'."));
        }

        List<String> errores = new ArrayList<>();
        List<JsonNode> nodosDientes = extraerNodosDientes(root, errores);
        if (nodosDientes.isEmpty()) {
            return new ValidationResult(List.of(), errores);
        }

        List<ValidatedToothDraft> dientesValidados = validarListaDeDientes(nodosDientes, errores);
        return new ValidationResult(dientesValidados, errores);
    }

    private JsonNode parsearJson(String rawAiResponse) {
        String sanitizedJson = limpiarBloquesMarkdown(rawAiResponse);
        try {
            return objectMapper.readTree(sanitizedJson);
        } catch (JacksonException ex) {
            return null;
        }
    }

    private List<ValidatedToothDraft> validarListaDeDientes(List<JsonNode> nodosDientes, List<String> errores) {
        List<ValidatedToothDraft> dientesValidados = new ArrayList<>();
        Set<Integer> dientesVistos = new HashSet<>();

        for (int idx = 0; idx < nodosDientes.size(); idx++) {
            validarNodoDiente(nodosDientes.get(idx), idx, dientesVistos, dientesValidados, errores);
        }
        return dientesValidados;
    }

    private List<JsonNode> extraerNodosDientes(JsonNode root, List<String> errores) {
        JsonNode dientesNode = root.get("dientes");
        if (dientesNode != null && dientesNode.isArray()) {
            if (dientesNode.isEmpty()) {
                errores.add("El arreglo 'dientes' está vacío. Debe incluir al menos un diente mencionado en el dictado.");
                return List.of();
            }
            List<JsonNode> list = new ArrayList<>();
            dientesNode.forEach(list::add);
            return list;
        }

        JsonNode dienteUnicoNode = root.get("diente");
        if (dienteUnicoNode != null && dienteUnicoNode.isObject()) {
            return List.of(dienteUnicoNode);
        }

        if (root.has("numero") || root.has("numeroDiente")) {
            return List.of(root);
        }

        errores.add("El JSON debe contener el arreglo 'dientes' con los objetos de cada diente mencionado.");
        return List.of();
    }

    private void validarNodoDiente(
            JsonNode dienteNode,
            int index,
            Set<Integer> dientesVistos,
            List<ValidatedToothDraft> dientesValidados,
            List<String> errores
    ) {
        String prefijo = "dientes[" + index + "]";
        if (dienteNode == null || !dienteNode.isObject()) {
            errores.add(prefijo + " debe ser un objeto JSON válido.");
            return;
        }

        Optional<DienteFdiMetadata> metadataOpt = extraerYValidarMetadataFdi(dienteNode, prefijo, dientesVistos, errores);
        if (metadataOpt.isEmpty()) {
            return;
        }

        DienteFdiMetadata metadata = metadataOpt.get();
        int erroresAntesDelDiente = errores.size();
        EstadoClinico estadoGeneral = extraerEstadoGeneral(dienteNode.get("estadoGeneral"), metadata.numero(), errores);

        JsonNode carasNode = dienteNode.get("caras");
        if (carasNode == null || carasNode.isNull() || !carasNode.isObject()) {
            if (errores.size() == erroresAntesDelDiente && estadoGeneral != null) {
                dientesValidados.add(ValidatedToothDraft.conEstadoCompleto(metadata, estadoGeneral));
            }
            return;
        }

        construirBorradorDesdeCaras(carasNode, metadata, estadoGeneral, erroresAntesDelDiente, dientesValidados, errores);
    }

    private Optional<DienteFdiMetadata> extraerYValidarMetadataFdi(
            JsonNode dienteNode,
            String prefijo,
            Set<Integer> dientesVistos,
            List<String> errores
    ) {
        JsonNode numeroNode = dienteNode.hasNonNull("numero")
                ? dienteNode.get("numero")
                : dienteNode.get("numeroDiente");

        if (numeroNode == null || !numeroNode.canConvertToInt()) {
            errores.add(prefijo + ".numero es obligatorio y debe ser un número entero FDI (ej. 15, 21, 36).");
            return Optional.empty();
        }

        int numero = numeroNode.asInt();
        Optional<DienteFdiMetadata> metadataOpt = CatalogoFdi.buscarPorNumero(numero);
        if (metadataOpt.isEmpty()) {
            errores.add("El número de diente " + numero + " en " + prefijo
                    + " NO existe en la nomenclatura FDI de 32 dientes permanentes (11-18, 21-28, 31-38, 41-48).");
            return Optional.empty();
        }

        if (!dientesVistos.add(numero)) {
            errores.add("El diente " + numero + " aparece duplicado. Consolida sus hallazgos en un único elemento.");
            return Optional.empty();
        }

        return metadataOpt;
    }

    private void construirBorradorDesdeCaras(
            JsonNode carasNode,
            DienteFdiMetadata metadata,
            EstadoClinico estadoGeneral,
            int erroresAntesDelDiente,
            List<ValidatedToothDraft> dientesValidados,
            List<String> errores
    ) {
        EstadoClinico mesial = extraerEstadoCaraConAutocorreccion(
                carasNode, LLAVES_MESIAL, "mesial", metadata, estadoGeneral, errores);
        EstadoClinico distal = extraerEstadoCaraConAutocorreccion(
                carasNode, LLAVES_DISTAL, "distal", metadata, estadoGeneral, errores);
        EstadoClinico vestibular = extraerEstadoCaraConAutocorreccion(
                carasNode, LLAVES_VESTIBULAR, "vestibular", metadata, estadoGeneral, errores);
        EstadoClinico palatinoLingual = extraerEstadoCaraConAutocorreccion(
                carasNode, LLAVES_CARA_INTERNA, "palatino_lingual", metadata, estadoGeneral, errores);
        EstadoClinico oclusalIncisal = extraerEstadoCaraConAutocorreccion(
                carasNode, LLAVES_CARA_MASTICATORIA, "oclusal_incisal", metadata, estadoGeneral, errores);

        if (errores.size() != erroresAntesDelDiente || estadoGeneral == null) {
            return;
        }

        // Si el LLM ubicó un estado de diente completo (ej. Corona, Endodoncia) dentro de una cara,
        // lo promovemos automáticamente a estadoGeneral sin forzar reintento.
        Optional<EstadoClinico> estadoCompletoEnCara = buscarEstadoDeDienteCompletoEnCaras(
                mesial, distal, vestibular, palatinoLingual, oclusalIncisal);
        if (estadoCompletoEnCara.isPresent() && !estadoGeneral.isInhabilitaCarasIndividuales()) {
            dientesValidados.add(ValidatedToothDraft.conEstadoCompleto(metadata, estadoCompletoEnCara.get()));
            return;
        }

        EstadoClinico generalFinal = normalizarEstadoGeneralSegunCaras(
                estadoGeneral, mesial, distal, vestibular, palatinoLingual, oclusalIncisal);

        dientesValidados.add(new ValidatedToothDraft(
                metadata,
                generalFinal,
                normalizarCaraConEstadoGeneral(mesial, generalFinal),
                normalizarCaraConEstadoGeneral(distal, generalFinal),
                normalizarCaraConEstadoGeneral(vestibular, generalFinal),
                normalizarCaraConEstadoGeneral(palatinoLingual, generalFinal),
                normalizarCaraConEstadoGeneral(oclusalIncisal, generalFinal)
        ));
    }

    private Optional<EstadoClinico> buscarEstadoDeDienteCompletoEnCaras(EstadoClinico... caras) {
        return Stream.of(caras)
                .filter(c -> c != null && c.isInhabilitaCarasIndividuales())
                .findFirst();
    }

    private EstadoClinico extraerEstadoGeneral(JsonNode estadoGeneralNode, int numeroDiente, List<String> errores) {
        if (estadoGeneralNode == null || estadoGeneralNode.isNull()) {
            return EstadoClinico.PRESENTE;
        }

        String textoEstado = extraerTextoDeNodoEstado(estadoGeneralNode);
        if (textoEstado == null || textoEstado.isBlank()) {
            errores.add("Diente " + numeroDiente + ": 'estadoGeneral' tiene un formato inválido.");
            return null;
        }

        return EstadoClinico.fromTexto(textoEstado)
                .orElseGet(() -> {
                    errores.add("Diente " + numeroDiente + ": estadoGeneral '" + textoEstado
                            + "' no es válido. Estados permitidos: [" + EstadoClinico.valoresPermitidosComoTexto() + "].");
                    return null;
                });
    }

    private EstadoClinico extraerEstadoCaraConAutocorreccion(
            JsonNode carasNode,
            List<String> llavesAceptadas,
            String nombreCanonicoCara,
            DienteFdiMetadata metadata,
            EstadoClinico estadoGeneral,
            List<String> errores
    ) {
        JsonNode caraNode = buscarPrimerNodoPresente(carasNode, llavesAceptadas);
        if (caraNode == null || caraNode.isNull()) {
            return (estadoGeneral != null && estadoGeneral.isInhabilitaCarasIndividuales())
                    ? estadoGeneral
                    : EstadoClinico.SANO;
        }

        String textoEstado = extraerTextoDeNodoEstado(caraNode);
        if (textoEstado == null || textoEstado.isBlank()) {
            errores.add("Diente " + metadata.numero() + ": la cara '" + nombreCanonicoCara
                    + "' no contiene un estado válido.");
            return null;
        }

        return EstadoClinico.fromTexto(textoEstado)
                .orElseGet(() -> {
                    errores.add("Diente " + metadata.numero() + ", cara '" + nombreCanonicoCara
                            + "': el estado '" + textoEstado + "' no existe. Valores permitidos: ["
                            + EstadoClinico.valoresPermitidosComoTexto() + "].");
                    return null;
                });
    }

    private JsonNode buscarPrimerNodoPresente(JsonNode carasNode, List<String> llavesCandidatas) {
        for (String llave : llavesCandidatas) {
            JsonNode nodo = carasNode.get(llave);
            if (nodo != null && !nodo.isNull()) {
                return nodo;
            }
        }
        return null;
    }

    private String extraerTextoDeNodoEstado(JsonNode nodo) {
        if (nodo.isString()) {
            return nodo.asString();
        }
        if (nodo.isObject() && nodo.hasNonNull("estado")) {
            return nodo.get("estado").asString();
        }
        return null;
    }

    private EstadoClinico normalizarEstadoGeneralSegunCaras(
            EstadoClinico estadoGeneral,
            EstadoClinico mesial,
            EstadoClinico distal,
            EstadoClinico vestibular,
            EstadoClinico palatinoLingual,
            EstadoClinico oclusalIncisal
    ) {
        if (estadoGeneral == null) {
            return EstadoClinico.PRESENTE;
        }
        if (estadoGeneral.isInhabilitaCarasIndividuales()) {
            return estadoGeneral;
        }

        boolean todasIguales = (mesial == distal)
                && (distal == vestibular)
                && (vestibular == palatinoLingual)
                && (palatinoLingual == oclusalIncisal);

        if (!todasIguales) {
            return EstadoClinico.PRESENTE;
        }
        return estadoGeneral;
    }

    private EstadoClinico normalizarCaraConEstadoGeneral(EstadoClinico estadoCara, EstadoClinico estadoGeneral) {
        if (estadoGeneral.isInhabilitaCarasIndividuales()) {
            return estadoGeneral;
        }
        return (estadoCara == EstadoClinico.PRESENTE) ? EstadoClinico.SANO : estadoCara;
    }

    private String limpiarBloquesMarkdown(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            int firstNewLine = trimmed.indexOf('\n');
            if (firstNewLine != -1) {
                trimmed = trimmed.substring(firstNewLine + 1);
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
        }
        return trimmed.trim();
    }
}
