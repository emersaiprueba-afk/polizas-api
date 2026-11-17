package com.santiagozarate.polizas.service;

import com.santiagozarate.polizas.dto.RiesgoRequest;
import com.santiagozarate.polizas.entity.Poliza;
import com.santiagozarate.polizas.entity.Riesgo;
import com.santiagozarate.polizas.exception.RecursoNoEncontradoException;
import com.santiagozarate.polizas.exception.ReglaNegocioException;
import com.santiagozarate.polizas.integration.PolizaActualizadaEvent;
import com.santiagozarate.polizas.repository.PolizaRepository;
import com.santiagozarate.polizas.repository.RiesgoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas del servicio con "dobles" (mocks): reemplazamos la base de datos
 * y el publicador de eventos por objetos falsos para probar solo la lógica del servicio.
 */
class PolizaServiceTest {

    private PolizaRepository polizaRepository;
    private RiesgoRepository riesgoRepository;
    private ApplicationEventPublisher eventos;
    private PolizaService service;

    @BeforeEach
    void preparar() {
        polizaRepository = mock(PolizaRepository.class);
        riesgoRepository = mock(RiesgoRepository.class);
        eventos = mock(ApplicationEventPublisher.class);
        Clock reloj = Clock.fixed(LocalDate.of(2026, 11, 17).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault());
        service = new PolizaService(polizaRepository, riesgoRepository, eventos, reloj, new BigDecimal("0.05"));
    }

    private Poliza individual() {
        return Poliza.individual("Tomador", LocalDate.of(2026, 1, 1), 12, new BigDecimal("1000000"),
                new Riesgo("Cra 1 # 2-3", "Arrendatario", "Arrendador"));
    }

    @Test
    void renovarPublicaElEventoParaElCore() {
        when(polizaRepository.findById(1L)).thenReturn(Optional.of(individual()));

        service.renovar(1L);

        verify(eventos).publishEvent(new PolizaActualizadaEvent(1L));
    }

    @Test
    void polizaInexistenteResponde404() {
        when(polizaRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> service.renovar(99L));
    }

    @Test
    void noSeAgreganRiesgosAPolizaIndividual() {
        when(polizaRepository.findById(1L)).thenReturn(Optional.of(individual()));
        RiesgoRequest request = new RiesgoRequest("Cra 1 # 2-3", "Arrendatario", "Arrendador");

        assertThrows(ReglaNegocioException.class, () -> service.agregarRiesgo(1L, request));
        verify(riesgoRepository, never()).save(any());
        verify(eventos, never()).publishEvent(any());
    }

    @Test
    void siFallaLaReglaNoSeAvisaAlCore() {
        Poliza cancelada = individual();
        cancelada.cancelar();
        when(polizaRepository.findById(1L)).thenReturn(Optional.of(cancelada));

        assertThrows(ReglaNegocioException.class, () -> service.renovar(1L));
        verify(eventos, never()).publishEvent(any());
    }
}
