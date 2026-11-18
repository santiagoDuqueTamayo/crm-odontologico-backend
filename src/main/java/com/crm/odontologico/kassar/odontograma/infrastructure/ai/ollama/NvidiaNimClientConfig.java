package com.crm.odontologico.kassar.odontograma.infrastructure.ai.ollama;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Configuración del cliente HTTP síncrono ({@link RestClient}) dedicado a la API de NVIDIA NIM.
 */
@Configuration
@ConditionalOnProperty(prefix = "odontograma.ai", name = "provider", havingValue = "nvidia-nim")
@EnableConfigurationProperties(NvidiaNimProperties.class)
public class NvidiaNimClientConfig {

    @Bean
    public RestClient nvidiaRestClient(NvidiaNimProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(properties.connectTimeoutSeconds()));
        requestFactory.setReadTimeout(Duration.ofSeconds(properties.readTimeoutSeconds()));

        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }
}
