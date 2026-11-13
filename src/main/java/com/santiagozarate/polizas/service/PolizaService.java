package com.santiagozarate.polizas.service;

import com.santiagozarate.polizas.dto.PolizaResponse;
import com.santiagozarate.polizas.dto.RiesgoRequest;
import com.santiagozarate.polizas.dto.RiesgoResponse;
import com.santiagozarate.polizas.entity.EstadoPoliza;
import com.santiagozarate.polizas.entity.Poliza;
import com.santiagozarate.polizas.entity.Riesgo;
import com.santiagozarate.polizas.entity.TipoPoliza;
import com.santiagozarate.polizas.exception.RecursoNoEncontradoException;
import com.santiagozarate.polizas.exception.ReglaNegocioException;
import com.santiagozarate.polizas.integration.CoreGateway;
import com.santiagozarate.polizas.repository.PolizaRepository;
import com.santiagozarate.polizas.repository.RiesgoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Casos de uso de pólizas. Orquesta: busca los datos, aplica las reglas
 * (que viven en las entidades), guarda y avisa al CORE.
 */
@Service
public class PolizaService {

    private final PolizaRepository polizaRepository;
    private final RiesgoRepository riesgoRepository;
    private final CoreGateway coreGateway;
    private final BigDecimal ipc;

    // Inyección por constructor: Spring entrega las dependencias al crear el servicio.
    public PolizaService(PolizaRepository polizaRepository,
                         RiesgoRepository riesgoRepository,
                         CoreGateway coreGateway,
                         @Value("${negocio.ipc}") BigDecimal ipc) {
        this.polizaRepository = polizaRepository;
        this.riesgoRepository = riesgoRepository;
        this.coreGateway = coreGateway;
        this.ipc = ipc;
    }

    @Transactional(readOnly = true)
    public List<PolizaResponse> listar(TipoPoliza tipo, EstadoPoliza estado) {
        return polizaRepository.buscar(tipo, estado).stream()
                .map(PolizaResponse::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RiesgoResponse> listarRiesgos(Long polizaId) {
        buscarPoliza(polizaId); // 404 si la póliza no existe
        return riesgoRepository.findByPolizaIdOrderById(polizaId).stream()
                .map(RiesgoResponse::desde)
                .toList();
    }

    @Transactional
    public PolizaResponse renovar(Long polizaId) {
        Poliza poliza = buscarPoliza(polizaId);
        poliza.renovar(ipc);
        coreGateway.notificarActualizacion(polizaId);
        return PolizaResponse.desde(poliza);
    }

    @Transactional
    public PolizaResponse cancelar(Long polizaId) {
        Poliza poliza = buscarPoliza(polizaId);
        poliza.cancelar();
        coreGateway.notificarActualizacion(polizaId);
        return PolizaResponse.desde(poliza);
    }

    @Transactional
    public RiesgoResponse agregarRiesgo(Long polizaId, RiesgoRequest request) {
        Poliza poliza = buscarPoliza(polizaId);
        if (poliza.getTipo() != TipoPoliza.COLECTIVA) {
            throw new ReglaNegocioException("Solo se pueden agregar riesgos a pólizas colectivas");
        }
        Riesgo riesgo = new Riesgo(request.direccionInmueble(),
                request.arrendatario(), request.arrendador());
        poliza.agregarRiesgo(riesgo);
        Riesgo guardado = riesgoRepository.save(riesgo);
        coreGateway.notificarActualizacion(polizaId);
        return RiesgoResponse.desde(guardado);
    }

    @Transactional
    public RiesgoResponse cancelarRiesgo(Long riesgoId) {
        Riesgo riesgo = riesgoRepository.findById(riesgoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Riesgo " + riesgoId + " no existe"));
        if (riesgo.estaCancelado()) {
            throw new ReglaNegocioException("El riesgo ya está cancelado");
        }
        riesgo.cancelar();
        coreGateway.notificarActualizacion(riesgo.getPoliza().getId());
        return RiesgoResponse.desde(riesgo);
    }

    private Poliza buscarPoliza(Long id) {
        return polizaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Póliza " + id + " no existe"));
    }
}
