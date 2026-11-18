package com.crm.odontologico.kassar.odontograma.application.mapper;

import com.crm.odontologico.kassar.odontograma.application.dto.CaraVisual;
import com.crm.odontologico.kassar.odontograma.application.dto.CarasDiente;
import com.crm.odontologico.kassar.odontograma.application.dto.DienteData;
import com.crm.odontologico.kassar.odontograma.application.dto.EstadoGeneral;
import com.crm.odontologico.kassar.odontograma.application.dto.OdontogramaUpdateResponse;
import com.crm.odontologico.kassar.odontograma.application.validation.ValidatedToothDraft;
import com.crm.odontologico.kassar.odontograma.domain.model.CatalogoFdi;
import com.crm.odontologico.kassar.odontograma.domain.model.CatalogoFdi.DienteFdiMetadata;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Consolida los dientes parciales reportados por el LLM con el catálogo completo de 32 dientes
 * de {@link CatalogoFdi}, asigna los estados por defecto ("Presente" y caras en "Sano") a las piezas
 * no mencionadas e inyecta los metadatos de UI ({@code colorHex}, {@code nombreColoquial},
 * {@code grupoDental} y etiquetas anatómicas exactas "Palatino" vs "Lingual", "Oclusal" vs "Incisal").
 */
@Component
public class OdontogramaDomainMapper {

    public OdontogramaUpdateResponse toUpdateResponse(
            String odontogramaId,
            List<ValidatedToothDraft> dientesReportadosPorLlm
    ) {
        List<ValidatedToothDraft> odontogramaConsolidado = consolidarConCatalogoCompleto(dientesReportadosPorLlm);
        List<DienteData> dientesEnriquecidos = odontogramaConsolidado.stream()
                .map(this::toDienteData)
                .toList();

        return new OdontogramaUpdateResponse(odontogramaId, dientesEnriquecidos);
    }

    public List<ValidatedToothDraft> consolidarConCatalogoCompleto(List<ValidatedToothDraft> dientesParciales) {
        Map<Integer, ValidatedToothDraft> reportadosPorNumero = indexarPorNumero(dientesParciales);

        return CatalogoFdi.obtenerTodos().values().stream()
                .map(metadata -> reportadosPorNumero.getOrDefault(
                        metadata.numero(),
                        ValidatedToothDraft.porDefecto(metadata)
                ))
                .toList();
    }

    public DienteData toDienteData(ValidatedToothDraft draft) {
        DienteFdiMetadata meta = draft.metadata();

        return new DienteData(
                meta.numero(),
                meta.nombreColoquial(),
                meta.cuadrante(),
                meta.grupo(),
                EstadoGeneral.de(draft.estadoGeneral()),
                construirCarasConMetadatosUi(draft, meta)
        );
    }

    private Map<Integer, ValidatedToothDraft> indexarPorNumero(List<ValidatedToothDraft> dientesParciales) {
        if (dientesParciales == null || dientesParciales.isEmpty()) {
            return Map.of();
        }
        return dientesParciales.stream()
                .collect(Collectors.toMap(
                        draft -> draft.metadata().numero(),
                        Function.identity(),
                        (primero, segundo) -> segundo
                ));
    }

    private CarasDiente construirCarasConMetadatosUi(ValidatedToothDraft draft, DienteFdiMetadata meta) {
        return new CarasDiente(
                CaraVisual.de(CatalogoFdi.ETIQUETA_MESIAL, draft.mesial()),
                CaraVisual.de(CatalogoFdi.ETIQUETA_DISTAL, draft.distal()),
                CaraVisual.de(CatalogoFdi.ETIQUETA_VESTIBULAR, draft.vestibular()),
                CaraVisual.de(meta.etiquetaCaraInterna(), draft.palatinoLingual()),
                CaraVisual.de(meta.etiquetaCaraMasticatoria(), draft.oclusalIncisal())
        );
    }
}
