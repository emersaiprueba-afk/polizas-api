package com.santiagozarate.polizas.service;

import com.santiagozarate.polizas.dto.RiesgoRequest;
import com.santiagozarate.polizas.entity.Poliza;
import com.santiagozarate.polizas.entity.TipoPoliza;
import com.santiagozarate.polizas.exception.RecursoNoEncontradoException;
import com.santiagozarate.polizas.exception.ReglaNegocioException;
import com.santiagozarate.polizas.integration.CoreGateway;
import com.santiagozarate.polizas.repository.PolizaRepository;
import com.santiagozarate.polizas.repository.RiesgoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas del servicio con "dobles" (mocks): reemplazamos la base de datos
 * y el CORE por objetos falsos para probar solo la lógica del servicio.
 */
class PolizaServiceTest {

    private PolizaRepository polizaRepository;
    private RiesgoRepository riesgoRepository;
    private CoreGateway coreGateway;
    private PolizaService service;

    @BeforeEach
    void preparar() {
        polizaRepository = mock(PolizaRepository.class);
        riesgoRepository = mock(RiesgoRepository.class);
        coreGateway = mock(CoreGateway.class);
        service = new PolizaService(polizaRepository, riesgoRepository, coreGateway, new BigDecimal("0.05"));
    }

    private Poliza poliza(TipoPoliza tipo) {
        return new Poliza(tipo, "Tomador", LocalDate.of(2026, 1, 1), 12, new BigDecimal("1000000"));
    }

    @Test
    void renovarNotificaAlCore() {
        when(polizaRepository.findById(1L)).thenReturn(Optional.of(poliza(TipoPoliza.INDIVIDUAL)));

        service.renovar(1L);

        verify(coreGateway).notificarActualizacion(1L);
    }

    @Test
    void polizaInexistenteResponde404() {
        when(polizaRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> service.renovar(99L));
    }

    @Test
    void noSeAgreganRiesgosAPolizaIndividualPorEsteEndpoint() {
        when(polizaRepository.findById(1L)).thenReturn(Optional.of(poliza(TipoPoliza.INDIVIDUAL)));
        RiesgoRequest request = new RiesgoRequest("Cra 1 # 2-3", "Arrendatario", "Arrendador");

        assertThrows(ReglaNegocioException.class, () -> service.agregarRiesgo(1L, request));
        verify(riesgoRepository, never()).save(any());
        verify(coreGateway, never()).notificarActualizacion(any());
    }

    @Test
    void siFallaLaReglaNoSeNotificaAlCore() {
        Poliza cancelada = poliza(TipoPoliza.COLECTIVA);
        cancelada.cancelar();
        when(polizaRepository.findById(1L)).thenReturn(Optional.of(cancelada));

        assertThrows(ReglaNegocioException.class, () -> service.renovar(1L));
        verify(coreGateway, never()).notificarActualizacion(any());
    }
}
