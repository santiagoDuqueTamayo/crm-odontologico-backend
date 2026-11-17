package com.crm.odontologico.kassar.odontograma;

import com.crm.odontologico.kassar.odontograma.application.dto.DienteData;
import com.crm.odontologico.kassar.odontograma.application.dto.OdontogramaUpdateResponse;
import com.crm.odontologico.kassar.odontograma.application.dto.VoiceCaptureRequest;
import com.crm.odontologico.kassar.odontograma.application.mapper.OdontogramaDomainMapper;
import com.crm.odontologico.kassar.odontograma.application.port.AiChatMessage;
import com.crm.odontologico.kassar.odontograma.application.port.OdontogramaAiPort;
import com.crm.odontologico.kassar.odontograma.application.service.OdontogramaPromptBuilder;
import com.crm.odontologico.kassar.odontograma.application.service.ProcessVoiceDictationService;
import com.crm.odontologico.kassar.odontograma.application.validation.AiResponseValidator;
import com.crm.odontologico.kassar.odontograma.domain.exception.AiValidationExhaustedException;
import com.crm.odontologico.kassar.odontograma.domain.model.EstadoClinico;
import com.crm.odontologico.kassar.odontograma.domain.model.GrupoDental;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessVoiceDictationServiceTest {

    private ObjectMapper objectMapper;
    private AiResponseValidator validator;
    private OdontogramaDomainMapper mapper;
    private OdontogramaPromptBuilder promptBuilder;

    @BeforeEach
    void setUp() {
        objectMapper = JsonMapper.builder().build();
        validator = new AiResponseValidator(objectMapper);
        mapper = new OdontogramaDomainMapper();
        promptBuilder = new OdontogramaPromptBuilder();
    }

    @Test
    @DisplayName("Debe consolidar el JSON parcial del LLM (1 diente) con los 32 dientes FDI, asignar estados por defecto e inyectar metadatos de UI")
    void debeConsolidarJsonParcialConLos32DientesEInyectarMetadatosUi() {
        OdontogramaAiPort fakeAiPort = messages -> """
                {
                  "dientes": [
                    {
                      "numero": 15,
                      "estadoGeneral": "Presente",
                      "caras": {
                        "mesial": "Sano",
                        "distal": "Sano",
                        "vestibular": "Sano",
                        "palatino_lingual": "Sano",
                        "oclusal_incisal": "Caries"
                      }
                    }
                  ]
                }
                """;

        ProcessVoiceDictationService service = new ProcessVoiceDictationService(
                fakeAiPort, promptBuilder, validator, mapper, objectMapper, 3
        );

        VoiceCaptureRequest request = new VoiceCaptureRequest(
                null,
                null,
                "{\"odontogramaId\": \"a1b2c3d4-5678\", \"textoCapturado\": \"  diente 15   oclusal caries  \"}"
        );

        OdontogramaUpdateResponse response = service.procesarDictado(request);

        assertEquals("a1b2c3d4-5678", response.odontogramaId());
        assertEquals(32, response.dientes().size());

        Map<Integer, DienteData> porNumero = indexarPorNumero(response);

        // Diente 15 reportado por el LLM con metadatos UI enriquecidos
        DienteData diente15 = porNumero.get(15);
        assertNotNull(diente15);
        assertEquals("Segundo premolar superior derecho", diente15.nombreColoquial());
        assertEquals(1, diente15.cuadrante());
        assertEquals(GrupoDental.POSTERIOR, diente15.grupoDental());
        assertEquals(EstadoClinico.PRESENTE, diente15.estadoGeneral().estado());
        assertNull(diente15.estadoGeneral().colorHex());

        assertEquals("Palatino", diente15.caras().palatinoLingual().etiquetaVisual());
        assertEquals(EstadoClinico.SANO, diente15.caras().palatinoLingual().estado());
        assertEquals("#22C55E", diente15.caras().palatinoLingual().colorHex());

        assertEquals("Oclusal", diente15.caras().oclusalIncisal().etiquetaVisual());
        assertEquals(EstadoClinico.CARIES, diente15.caras().oclusalIncisal().estado());
        assertEquals("#EF4444", diente15.caras().oclusalIncisal().colorHex());

        // Dientes no reportados por el LLM deben tener estado por defecto (Presente / caras Sano)
        DienteData diente21NoReportado = porNumero.get(21);
        assertNotNull(diente21NoReportado);
        assertEquals("Incisivo central superior izquierdo", diente21NoReportado.nombreColoquial());
        assertEquals(GrupoDental.ANTERIOR, diente21NoReportado.grupoDental());
        assertEquals(EstadoClinico.PRESENTE, diente21NoReportado.estadoGeneral().estado());
        assertEquals(EstadoClinico.SANO, diente21NoReportado.caras().oclusalIncisal().estado());
        assertEquals("Incisal", diente21NoReportado.caras().oclusalIncisal().etiquetaVisual());
        assertEquals("Palatino", diente21NoReportado.caras().palatinoLingual().etiquetaVisual());
    }

    @Test
    @DisplayName("Debe autocorregir errores anatómicos del LLM (ej. cara 'oclusal' en diente anterior 11 a 'Incisal') sin reintentar")
    void debeAutocorregirCaraOclusalEnDienteAnteriorSinReintentar() {
        AtomicInteger contadorLlamadas = new AtomicInteger(0);

        OdontogramaAiPort aiPortConErrorAnatomico = messages -> {
            contadorLlamadas.incrementAndGet();
            // El LLM devuelve "oclusal" y "lingual" para el diente 11 (Anterior Superior)
            return """
                    {
                      "dientes": [
                        {
                          "numero": 11,
                          "caras": {
                            "oclusal": "Fracturado",
                            "lingual": "Caries"
                          }
                        }
                      ]
                    }
                    """;
        };

        ProcessVoiceDictationService service = new ProcessVoiceDictationService(
                aiPortConErrorAnatomico, promptBuilder, validator, mapper, objectMapper, 3
        );

        OdontogramaUpdateResponse response = service.procesarDictado(
                new VoiceCaptureRequest("odo-11", "diente 11 fracturado en borde y caries interna")
        );

        // No debe obligar al LLM a reintentar: se procesa en 1 sola llamada
        assertEquals(1, contadorLlamadas.get());
        assertEquals(32, response.dientes().size());

        Map<Integer, DienteData> porNumero = indexarPorNumero(response);
        DienteData diente11 = porNumero.get(11);

        assertEquals("Incisivo central superior derecho", diente11.nombreColoquial());
        assertEquals(GrupoDental.ANTERIOR, diente11.grupoDental());
        assertEquals(EstadoClinico.PRESENTE, diente11.estadoGeneral().estado());

        // "oclusal" fue mapeado automáticamente a "Incisal" y "lingual" a "Palatino"
        assertEquals("Incisal", diente11.caras().oclusalIncisal().etiquetaVisual());
        assertEquals(EstadoClinico.FRACTURADO, diente11.caras().oclusalIncisal().estado());
        assertEquals("#F97316", diente11.caras().oclusalIncisal().colorHex());

        assertEquals("Palatino", diente11.caras().palatinoLingual().etiquetaVisual());
        assertEquals(EstadoClinico.CARIES, diente11.caras().palatinoLingual().estado());
        assertEquals("#EF4444", diente11.caras().palatinoLingual().colorHex());

        // Caras no mencionadas en el diente 11 toman "Sano" por defecto
        assertEquals(EstadoClinico.SANO, diente11.caras().mesial().estado());
        assertEquals(EstadoClinico.SANO, diente11.caras().distal().estado());
        assertEquals(EstadoClinico.SANO, diente11.caras().vestibular().estado());
    }

    @Test
    @DisplayName("Debe distinguir Lingual e Incisal en dientes anteriores inferiores y soportar estados de diente completo")
    void debeSoportarMultiplesDientesConAnatomiaFdiExacta() {
        OdontogramaAiPort fakeAiPort = messages -> """
                {
                  "dientes": [
                    {
                      "numero": 31,
                      "estadoGeneral": "Presente",
                      "caras": {
                        "mesial": "Sano",
                        "distal": "Sano",
                        "vestibular": "Restaurado",
                        "palatino_lingual": "Sano",
                        "oclusal_incisal": "Fracturado"
                      }
                    },
                    {
                      "numero": 46,
                      "estadoGeneral": "Ausente"
                    }
                  ]
                }
                """;

        ProcessVoiceDictationService service = new ProcessVoiceDictationService(
                fakeAiPort, promptBuilder, validator, mapper, objectMapper, 3
        );

        VoiceCaptureRequest request = new VoiceCaptureRequest(
                "odo-999",
                "incisivo central inferior izquierdo vestibular restaurado, borde incisal fracturado, y primer molar inferior derecho ausente"
        );

        OdontogramaUpdateResponse response = service.procesarDictado(request);

        assertEquals(32, response.dientes().size());
        Map<Integer, DienteData> porNumero = indexarPorNumero(response);

        DienteData diente31 = porNumero.get(31);
        assertEquals("Incisivo central inferior izquierdo", diente31.nombreColoquial());
        assertEquals(GrupoDental.ANTERIOR, diente31.grupoDental());
        assertEquals("Lingual", diente31.caras().palatinoLingual().etiquetaVisual());
        assertEquals("Incisal", diente31.caras().oclusalIncisal().etiquetaVisual());
        assertEquals(EstadoClinico.FRACTURADO, diente31.caras().oclusalIncisal().estado());
        assertEquals(EstadoClinico.RESTAURADO, diente31.caras().vestibular().estado());

        DienteData diente46 = porNumero.get(46);
        assertEquals("Primer molar inferior derecho", diente46.nombreColoquial());
        assertEquals(EstadoClinico.AUSENTE, diente46.estadoGeneral().estado());
        assertEquals("#6B7280", diente46.estadoGeneral().colorHex());
        assertEquals(EstadoClinico.AUSENTE, diente46.caras().oclusalIncisal().estado());
    }

    @Test
    @DisplayName("Debe reintentar con feedback al LLM cuando devuelve un número FDI inexistente y consolidar los 32 dientes tras corregirse")
    void debeReintentarCuandoElLlmDevuelveNumeroFdiInvalido() {
        AtomicInteger contadorLlamadas = new AtomicInteger(0);
        List<List<AiChatMessage>> historialesRecibidos = new ArrayList<>();

        OdontogramaAiPort selfCorrectingAiPort = messages -> {
            historialesRecibidos.add(List.copyOf(messages));
            int intento = contadorLlamadas.incrementAndGet();
            if (intento == 1) {
                return """
                        {
                          "dientes": [
                            {
                              "numero": 19,
                              "estadoGeneral": "Presente",
                              "caras": {
                                "oclusal": "Caries"
                              }
                            }
                          ]
                        }
                        """;
            }
            return """
                    {
                      "dientes": [
                        {
                          "numero": 18,
                          "estadoGeneral": "Presente",
                          "caras": {
                            "oclusal_incisal": "Caries"
                          }
                        }
                      ]
                    }
                    """;
        };

        ProcessVoiceDictationService service = new ProcessVoiceDictationService(
                selfCorrectingAiPort, promptBuilder, validator, mapper, objectMapper, 3
        );

        OdontogramaUpdateResponse response = service.procesarDictado(
                new VoiceCaptureRequest("odo-18", "cordal superior derecha con caries oclusal")
        );

        assertEquals(2, contadorLlamadas.get());
        assertEquals(32, response.dientes().size());

        Map<Integer, DienteData> porNumero = indexarPorNumero(response);
        assertEquals(EstadoClinico.CARIES, porNumero.get(18).caras().oclusalIncisal().estado());

        List<AiChatMessage> segundoHistorial = historialesRecibidos.get(1);
        assertEquals(4, segundoHistorial.size());
        assertTrue(segundoHistorial.get(3).content().contains("19"));
    }

    @Test
    @DisplayName("Debe lanzar AiValidationExhaustedException si el modelo no corrige errores estructurales o FDI tras agotar intentos")
    void debeLanzarExcepcionCuandoSeAgotanLosIntentosDeCorreccion() {
        OdontogramaAiPort brokenAiPort = messages -> "{ \"dientes\": [ { \"numero\": 99 } ] }";

        ProcessVoiceDictationService service = new ProcessVoiceDictationService(
                brokenAiPort, promptBuilder, validator, mapper, objectMapper, 2
        );

        AiValidationExhaustedException ex = assertThrows(
                AiValidationExhaustedException.class,
                () -> service.procesarDictado(new VoiceCaptureRequest("odo-err", "diente 99 sano"))
        );

        assertEquals(2, ex.getIntentosRealizados());
        assertNotNull(ex.getUltimosErrores());
        assertTrue(ex.getUltimosErrores().get(0).contains("99"));
    }

    @Test
    @DisplayName("Debe soportar todos los nuevos estados clínicos y sus sinónimos con sus colores hexadecimales")
    void debeSoportarNuevosEstadosClinicosYSinonimos() {
        OdontogramaAiPort aiPort = messages -> """
                {
                  "dientes": [
                    {
                      "numero": 12,
                      "estadoGeneral": "Presente",
                      "caras": {
                        "vestibular": "Desgaste/Abfracción",
                        "distal": "Rest. Defectuosa",
                        "oclusal_incisal": "Obturación Temporal"
                      }
                    },
                    { "numero": 13, "estadoGeneral": "Endodoncia" },
                    { "numero": 14, "estadoGeneral": "Núcleo/Poste" },
                    { "numero": 15, "estadoGeneral": "Corona" },
                    { "numero": 16, "estadoGeneral": "Póntico" },
                    { "numero": 17, "estadoGeneral": "Prótesis Removible" },
                    { "numero": 26, "estadoGeneral": "Resto Radicular" },
                    { "numero": 38, "estadoGeneral": "En Erupción" }
                  ]
                }
                """;

        ProcessVoiceDictationService service = new ProcessVoiceDictationService(
                aiPort, promptBuilder, validator, mapper, objectMapper, 3
        );

        OdontogramaUpdateResponse response = service.procesarDictado(
                new VoiceCaptureRequest("odo-nuevos-estados", "dictado con nuevos estados")
        );

        Map<Integer, DienteData> porNumero = indexarPorNumero(response);

        DienteData d12 = porNumero.get(12);
        assertEquals(EstadoClinico.DESGASTE, d12.caras().vestibular().estado());
        assertEquals("#FDBA74", d12.caras().vestibular().colorHex());
        assertEquals(EstadoClinico.DEFECTUOSO, d12.caras().distal().estado());
        assertEquals("#E11D48", d12.caras().distal().colorHex());
        assertEquals(EstadoClinico.TEMPORAL, d12.caras().oclusalIncisal().estado());
        assertEquals("#94A3B8", d12.caras().oclusalIncisal().colorHex());

        assertEquals(EstadoClinico.ENDODONCIA, porNumero.get(13).estadoGeneral().estado());
        assertEquals("#D946EF", porNumero.get(13).estadoGeneral().colorHex());
        assertEquals(EstadoClinico.ENDODONCIA, porNumero.get(13).caras().oclusalIncisal().estado());

        assertEquals(EstadoClinico.NUCLEO, porNumero.get(14).estadoGeneral().estado());
        assertEquals("#8B5CF6", porNumero.get(14).estadoGeneral().colorHex());

        assertEquals(EstadoClinico.CORONA, porNumero.get(15).estadoGeneral().estado());
        assertEquals("#EAB308", porNumero.get(15).estadoGeneral().colorHex());
        assertEquals(EstadoClinico.CORONA, porNumero.get(15).caras().mesial().estado());

        assertEquals(EstadoClinico.PONTICO, porNumero.get(16).estadoGeneral().estado());
        assertEquals("#FBBF24", porNumero.get(16).estadoGeneral().colorHex());

        assertEquals(EstadoClinico.PROTESIS_REMOVIBLE, porNumero.get(17).estadoGeneral().estado());
        assertEquals("#F472B6", porNumero.get(17).estadoGeneral().colorHex());

        assertEquals(EstadoClinico.RESTO_RADICULAR, porNumero.get(26).estadoGeneral().estado());
        assertEquals("#78716C", porNumero.get(26).estadoGeneral().colorHex());

        assertEquals(EstadoClinico.EN_ERUPCION, porNumero.get(38).estadoGeneral().estado());
        assertEquals("#84CC16", porNumero.get(38).estadoGeneral().colorHex());
    }

    @Test
    @DisplayName("Debe mantener las 5 caras en Sano cuando el diente tiene una lesión exclusiva de la raíz (ej. Fractura Radicular, Lesión Periapical)")
    void debeMantenerCarasEnSanoCuandoEsLesionExclusivaDeRaiz() {
        OdontogramaAiPort aiPort = messages -> """
                {
                  "dientes": [
                    { "numero": 21, "estadoGeneral": "Fractura Radicular" },
                    { "numero": 22, "estadoGeneral": "Lesión Periapical" },
                    { "numero": 23, "estadoGeneral": "Apicectomía" },
                    { "numero": 36, "estadoGeneral": "Lesión de Furca" },
                    { "numero": 46, "estadoGeneral": "Reabsorción Radicular" },
                    { "numero": 18, "estadoGeneral": "Anomalía Radicular" }
                  ]
                }
                """;

        ProcessVoiceDictationService service = new ProcessVoiceDictationService(
                aiPort, promptBuilder, validator, mapper, objectMapper, 3
        );

        OdontogramaUpdateResponse response = service.procesarDictado(
                new VoiceCaptureRequest("odo-raiz", "dictado con lesiones radiculares")
        );

        Map<Integer, DienteData> porNumero = indexarPorNumero(response);

        DienteData d21 = porNumero.get(21);
        assertEquals(EstadoClinico.FRACTURA_RADICULAR, d21.estadoGeneral().estado());
        assertEquals("#B91C1C", d21.estadoGeneral().colorHex());
        assertEquals(EstadoClinico.SANO, d21.caras().mesial().estado());
        assertEquals(EstadoClinico.SANO, d21.caras().oclusalIncisal().estado());

        assertEquals(EstadoClinico.LESION_PERIAPICAL, porNumero.get(22).estadoGeneral().estado());
        assertEquals(EstadoClinico.SANO, porNumero.get(22).caras().vestibular().estado());

        assertEquals(EstadoClinico.APICECTOMIA, porNumero.get(23).estadoGeneral().estado());
        assertEquals(EstadoClinico.LESION_FURCA, porNumero.get(36).estadoGeneral().estado());
        assertEquals(EstadoClinico.REABSORCION_RADICULAR, porNumero.get(46).estadoGeneral().estado());
        assertEquals(EstadoClinico.ANOMALIA_RADICULAR, porNumero.get(18).estadoGeneral().estado());
    }

    private Map<Integer, DienteData> indexarPorNumero(OdontogramaUpdateResponse response) {
        return response.dientes().stream()
                .collect(Collectors.toMap(DienteData::numero, Function.identity()));
    }
}
