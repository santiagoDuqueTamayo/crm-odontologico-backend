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

/**
 * Ensambla autoritativamente el contrato {@link OdontogramaUpdateResponse} hacia el Frontend
 * utilizando los metadatos anatómicos del {@link CatalogoFdi} (nombre coloquial, Palatino vs Lingual,
 * Oclusal vs Incisal) y los colores oficiales de cada estado clínico.
 */
@Component
public class OdontogramaDomainMapper {

    public OdontogramaUpdateResponse toUpdateResponse(String odontogramaId, List<ValidatedToothDraft> dientesValidados) {
        List<DienteData> dientes = dientesValidados.stream()
                .map(this::toDienteData)
                .toList();
        return new OdontogramaUpdateResponse(odontogramaId, dientes);
    }

    public DienteData toDienteData(ValidatedToothDraft draft) {
        DienteFdiMetadata meta = draft.metadata();

        CarasDiente caras = new CarasDiente(
                CaraVisual.de(CatalogoFdi.ETIQUETA_MESIAL, draft.mesial()),
                CaraVisual.de(CatalogoFdi.ETIQUETA_DISTAL, draft.distal()),
                CaraVisual.de(CatalogoFdi.ETIQUETA_VESTIBULAR, draft.vestibular()),
                CaraVisual.de(meta.etiquetaCaraInterna(), draft.palatinoLingual()),
                CaraVisual.de(meta.etiquetaCaraMasticatoria(), draft.oclusalIncisal())
        );

        return new DienteData(
                meta.numero(),
                meta.nombreColoquial(),
                meta.cuadrante(),
                meta.grupo(),
                EstadoGeneral.de(draft.estadoGeneral()),
                caras
        );
    }
}
