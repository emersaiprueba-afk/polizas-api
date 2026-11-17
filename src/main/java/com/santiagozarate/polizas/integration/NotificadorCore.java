package com.santiagozarate.polizas.integration;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Avisa al CORE solo DESPUÉS de que la transacción se confirma (commit).
 * Si el guardado en base de datos falla, el evento se descarta y el CORE
 * nunca recibe un cambio que no ocurrió.
 */
@Component
public class NotificadorCore {

    private final CoreGateway coreGateway;

    public NotificadorCore(CoreGateway coreGateway) {
        this.coreGateway = coreGateway;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alConfirmar(PolizaActualizadaEvent evento) {
        coreGateway.notificarActualizacion(evento.polizaId());
    }
}
