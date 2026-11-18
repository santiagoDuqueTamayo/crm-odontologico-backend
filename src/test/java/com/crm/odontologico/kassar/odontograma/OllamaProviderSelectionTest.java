package com.crm.odontologico.kassar.odontograma;

import com.crm.odontologico.kassar.odontograma.application.port.OdontogramaAiPort;
import com.crm.odontologico.kassar.odontograma.infrastructure.ai.ollama.NvidiaNimAiAdapter;
import com.crm.odontologico.kassar.odontograma.infrastructure.ai.ollama.OllamaAiAdapter;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class OllamaProviderSelectionTest {

    @Nested
    @SpringBootTest(properties = "odontograma.ai.provider=ollama")
    class CuandoProveedorEsOllama {

        @Autowired
        private OdontogramaAiPort aiPort;

        @Test
        void debeSeleccionarOllamaSinNecesitarLaClaveDeGoogle() {
            assertInstanceOf(OllamaAiAdapter.class, aiPort);
        }
    }

    @Nested
    @SpringBootTest(properties = "odontograma.ai.provider=nvidia-nim")
    class CuandoProveedorEsNvidiaNim {

        @Autowired
        private OdontogramaAiPort aiPort;

        @Test
        void debeSeleccionarNvidiaNimSinNecesitarLaClaveDeGoogle() {
            assertInstanceOf(NvidiaNimAiAdapter.class, aiPort);
        }
    }
}
