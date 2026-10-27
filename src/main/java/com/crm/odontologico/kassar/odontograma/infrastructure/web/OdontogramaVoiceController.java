package com.crm.odontologico.kassar.odontograma.infrastructure.web;

import com.crm.odontologico.kassar.odontograma.application.dto.OdontogramaUpdateResponse;
import com.crm.odontologico.kassar.odontograma.application.dto.VoiceCaptureRequest;
import com.crm.odontologico.kassar.odontograma.application.service.ProcessVoiceDictationService;
import com.crm.odontologico.kassar.odontograma.domain.model.CatalogoFdi;
import com.crm.odontologico.kassar.odontograma.domain.model.CatalogoFdi.DienteFdiMetadata;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

/**
 * Controlador REST de entrada para el procesamiento de dictado por voz del Odontograma.
 */
@RestController
@RequestMapping("/api/v1/odontogramas")
@CrossOrigin(origins = "*")
public class OdontogramaVoiceController {

    private final ProcessVoiceDictationService processVoiceDictationService;

    public OdontogramaVoiceController(ProcessVoiceDictationService processVoiceDictationService) {
        this.processVoiceDictationService = processVoiceDictationService;
    }

    /**
     * Recibe la captura de voz proveniente del Frontend, la procesa con el modelo de IA en Ollama,
     * valida clínica y anatómicamente la respuesta (con bucle de autocorrección si hay errores)
     * y retorna el contrato actualizado para el Odontograma.
     */
    @PostMapping(
            value = "/voice-capture",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<OdontogramaUpdateResponse> procesarDictadoVoz(
            @RequestBody VoiceCaptureRequest request
    ) {
        OdontogramaUpdateResponse response = processVoiceDictationService.procesarDictado(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint de consulta del catálogo oficial FDI de 32 dientes permanentes con sus nombres
     * coloquiales y etiquetas anatómicas exactas por cuadrante y grupo dental.
     */
    @GetMapping(value = "/catalogo-fdi", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Collection<DienteFdiMetadata>> obtenerCatalogoFdi() {
        return ResponseEntity.ok(CatalogoFdi.obtenerTodos().values());
    }
}
