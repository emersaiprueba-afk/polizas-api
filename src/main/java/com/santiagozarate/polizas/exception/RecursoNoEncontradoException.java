package com.santiagozarate.polizas.exception;

/** Se lanza cuando no existe la póliza o el riesgo pedido (respuesta HTTP 404). */
public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
