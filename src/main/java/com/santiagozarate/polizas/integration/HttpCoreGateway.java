package com.santiagozarate.polizas.integration;

import com.santiagozarate.polizas.dto.EventoCoreRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

/**
 * Adaptador que envía el evento al servicio de edición del CORE por HTTP.
 * En esta prueba apunta al mock POST /core-mock/evento; en producción
 * apuntaría a la capa media en WebLogic cambiando solo la configuración.
 */
@Component
public class HttpCoreGateway implements CoreGateway {

    private static final Logger log = LoggerFactory.getLogger(HttpCoreGateway.class);

    private final RestClient restClient;
    private final String coreUrl;
    private final String apiKey;

    public HttpCoreGateway(@Value("${core.url}") String coreUrl,
                           @Value("${seguridad.api-key}") String apiKey,
                           @Value("${core.timeout-ms:3000}") long timeoutMs) {
        // Timeouts: si el CORE no responde, no dejamos hilos colgados indefinidamente.
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofMillis(timeoutMs));
        fabrica.setReadTimeout(Duration.ofMillis(timeoutMs));
        this.restClient = RestClient.builder().requestFactory(fabrica).build();
        this.coreUrl = coreUrl;
        this.apiKey = apiKey;
    }

    @Override
    public void notificarActualizacion(Long polizaId) {
        try {
            restClient.post()
                    .uri(coreUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("x-api-key", apiKey)
                    .body(new EventoCoreRequest("ACTUALIZACION", polizaId))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Evento ACTUALIZACION enviado al CORE para la póliza {}", polizaId);
        } catch (RestClientException ex) {
            // El cambio ya está confirmado en base de datos; no se revierte al usuario.
            // En producción: patrón outbox con reintentos y conciliación con el CORE.
            log.error("No se pudo notificar al CORE la póliza {}: {}", polizaId, ex.getMessage());
        }
    }
}
