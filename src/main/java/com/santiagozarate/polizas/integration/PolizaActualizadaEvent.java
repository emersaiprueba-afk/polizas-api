package com.santiagozarate.polizas.integration;

/** Evento interno: una póliza (o uno de sus riesgos) cambió y el CORE debe enterarse. */
public record PolizaActualizadaEvent(Long polizaId) {
}
