package com.santiagozarate.polizas.controller;

import com.santiagozarate.polizas.dto.EventoCoreRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mock del servicio de edición del CORE (capa media en WebLogic).
 * Su único propósito: dejar en el log que la operación se intentó enviar al CORE.
 */
@RestController
@RequestMapping("/core-mock")
public class CoreMockController {

    private static final Logger log = LoggerFactory.getLogger(CoreMockController.class);

    @PostMapping("/evento")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void recibirEvento(@Valid @RequestBody EventoCoreRequest evento) {
        log.info("[CORE-MOCK] Evento recibido: evento={}, polizaId={}",
                evento.evento(), evento.polizaId());
    }
}
