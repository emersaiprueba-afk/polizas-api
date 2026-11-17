package com.santiagozarate.polizas.service;

import com.santiagozarate.polizas.dto.PolizaResponse;
import com.santiagozarate.polizas.dto.RiesgoRequest;
import com.santiagozarate.polizas.dto.RiesgoResponse;
import com.santiagozarate.polizas.entity.EstadoPoliza;
import com.santiagozarate.polizas.entity.Poliza;
import com.santiagozarate.polizas.entity.Riesgo;
import com.santiagozarate.polizas.entity.TipoPoliza;
import com.santiagozarate.polizas.exception.RecursoNoEncontradoException;
import com.santiagozarate.polizas.integration.PolizaActualizadaEvent;
import com.santiagozarate.polizas.repository.PolizaRepository;
import com.santiagozarate.polizas.repository.RiesgoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * Casos de uso de pólizas. Orquesta: busca los datos, delega las reglas en la
 * entidad Poliza, guarda y publica un evento para avisar al CORE después del commit.
 */
@Service
public class PolizaService {

    private final PolizaRepository polizaRepository;
    private final RiesgoRepository riesgoRepository;
    private final ApplicationEventPublisher eventos;
    private final Clock reloj;
    private final BigDecimal ipc;

    // Inyección por constructor: Spring entrega las dependencias al crear el servicio.
    public PolizaService(PolizaRepository polizaRepository,
                         RiesgoRepository riesgoRepository,
                         ApplicationEventPublisher eventos,
                         Clock reloj,
                         @Value("${negocio.ipc}") BigDecimal ipc) {
        this.polizaRepository = polizaRepository;
        this.riesgoRepository = riesgoRepository;
        this.eventos = eventos;
        this.reloj = reloj;
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
        poliza.renovar(ipc, LocalDate.now(reloj));
        avisarCore(polizaId);
        return PolizaResponse.desde(poliza);
    }

    @Transactional
    public PolizaResponse cancelar(Long polizaId) {
        Poliza poliza = buscarPoliza(polizaId);
        poliza.cancelar();
        avisarCore(polizaId);
        return PolizaResponse.desde(poliza);
    }

    @Transactional
    public RiesgoResponse agregarRiesgo(Long polizaId, RiesgoRequest request) {
        Poliza poliza = buscarPoliza(polizaId);
        Riesgo riesgo = new Riesgo(request.direccionInmueble(),
                request.arrendatario(), request.arrendador());
        poliza.agregarRiesgo(riesgo); // valida: póliza colectiva y no cancelada
        Riesgo guardado = riesgoRepository.save(riesgo);
        avisarCore(polizaId);
        return RiesgoResponse.desde(guardado);
    }

    @Transactional
    public RiesgoResponse cancelarRiesgo(Long riesgoId) {
        Riesgo riesgo = riesgoRepository.findById(riesgoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Riesgo " + riesgoId + " no existe"));
        Poliza poliza = riesgo.getPoliza();
        poliza.cancelarRiesgo(riesgo); // la regla vive en la póliza, raíz del agregado
        avisarCore(poliza.getId());
        return RiesgoResponse.desde(riesgo);
    }

    /** El evento se entrega al CORE solo si la transacción se confirma (ver NotificadorCore). */
    private void avisarCore(Long polizaId) {
        eventos.publishEvent(new PolizaActualizadaEvent(polizaId));
    }

    private Poliza buscarPoliza(Long id) {
        return polizaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Póliza " + id + " no existe"));
    }
}
