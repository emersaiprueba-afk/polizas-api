package com.santiagozarate.polizas.integration;

/**
 * Puerto de salida hacia el CORE de seguros (patrón hexagonal / adapter).
 * El servicio de negocio solo conoce esta interfaz; no sabe si detrás hay
 * un mock, un servicio SOAP en WebLogic o una cola de mensajes.
 */
public interface CoreGateway {

    void notificarActualizacion(Long polizaId);
}
