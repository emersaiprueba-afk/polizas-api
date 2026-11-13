package com.santiagozarate.polizas.integration;

import com.santiagozarate.polizas.dto.EventoCoreRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

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
                           @Value("${seguridad.api-key}") String apiKey) {
        this.restClient = RestClient.create();
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
        } catch (RestClientException ex) {
            // No tumbamos la operación del usuario si el CORE no responde.
            // En producción: guardar el evento en una tabla outbox y reintentar.
            log.error("No se pudo notificar al CORE la póliza {}: {}", polizaId, ex.getMessage());
        }
    }
}
