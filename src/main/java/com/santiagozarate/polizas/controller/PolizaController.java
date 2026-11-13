package com.santiagozarate.polizas.controller;

import com.santiagozarate.polizas.dto.PolizaResponse;
import com.santiagozarate.polizas.dto.RiesgoRequest;
import com.santiagozarate.polizas.dto.RiesgoResponse;
import com.santiagozarate.polizas.entity.EstadoPoliza;
import com.santiagozarate.polizas.entity.TipoPoliza;
import com.santiagozarate.polizas.service.PolizaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Capa de entrada HTTP. Solo traduce peticiones a llamadas del servicio:
 * no contiene reglas de negocio.
 */
@RestController
@RequestMapping("/polizas")
public class PolizaController {

    private final PolizaService polizaService;

    public PolizaController(PolizaService polizaService) {
        this.polizaService = polizaService;
    }

    /** GET /polizas?tipo=COLECTIVA&estado=ACTIVA (ambos filtros opcionales). */
    @GetMapping
    public List<PolizaResponse> listar(@RequestParam(required = false) TipoPoliza tipo,
                                       @RequestParam(required = false) EstadoPoliza estado) {
        return polizaService.listar(tipo, estado);
    }

    @GetMapping("/{id}/riesgos")
    public List<RiesgoResponse> riesgos(@PathVariable Long id) {
        return polizaService.listarRiesgos(id);
    }

    @PostMapping("/{id}/renovar")
    public PolizaResponse renovar(@PathVariable Long id) {
        return polizaService.renovar(id);
    }

    @PostMapping("/{id}/cancelar")
    public PolizaResponse cancelar(@PathVariable Long id) {
        return polizaService.cancelar(id);
    }

    @PostMapping("/{id}/riesgos")
    @ResponseStatus(HttpStatus.CREATED)
    public RiesgoResponse agregarRiesgo(@PathVariable Long id,
                                        @Valid @RequestBody RiesgoRequest request) {
        return polizaService.agregarRiesgo(id, request);
    }
}
