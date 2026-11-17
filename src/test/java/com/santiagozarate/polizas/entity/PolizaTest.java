package com.santiagozarate.polizas.entity;

import com.santiagozarate.polizas.exception.ReglaNegocioException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pruebas de las reglas de negocio puras: no necesitan Spring ni base de datos. */
class PolizaTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 1, 1);
    private static final LocalDate HOY = LocalDate.of(2026, 11, 17);
    private static final BigDecimal IPC = new BigDecimal("0.05");

    private Riesgo nuevoRiesgo() {
        return new Riesgo("Cra 1 # 2-3", "Arrendatario", "Arrendador");
    }

    private Poliza individual() {
        return Poliza.individual("Tomador", INICIO, 12, new BigDecimal("1000000"), nuevoRiesgo());
    }

    private Poliza colectiva() {
        return Poliza.colectiva("Inmobiliaria", INICIO, 12, new BigDecimal("1000000"));
    }

    @Test
    void primaEsCanonPorMesesDeVigencia() {
        assertEquals(new BigDecimal("12000000.00"), individual().getPrima());
    }

    @Test
    void individualNaceConUnRiesgoYNoAdmiteMas() {
        Poliza poliza = individual();
        assertEquals(1, poliza.riesgosActivos());
        assertThrows(ReglaNegocioException.class, () -> poliza.agregarRiesgo(nuevoRiesgo()));
    }

    @Test
    void colectivaAdmiteVariosRiesgos() {
        Poliza poliza = colectiva();
        poliza.agregarRiesgo(nuevoRiesgo());
        poliza.agregarRiesgo(nuevoRiesgo());
        assertEquals(2, poliza.riesgosActivos());
    }

    @Test
    void renovarAjustaCanonYPrimaPorIpcYCambiaEstado() {
        Poliza poliza = individual();
        poliza.renovar(IPC, HOY);

        assertEquals(new BigDecimal("1050000.00"), poliza.getCanonMensual());
        assertEquals(new BigDecimal("12600000.00"), poliza.getPrima());
        assertEquals(EstadoPoliza.RENOVADA, poliza.getEstado());
        assertEquals(LocalDate.of(2027, 1, 1), poliza.getFechaInicio());
        assertEquals(LocalDate.of(2028, 1, 1), poliza.getFechaFin());
    }

    @Test
    void renovarDosVecesSeguidasNoAplicaElIpcDosVeces() {
        Poliza poliza = individual();
        poliza.renovar(IPC, HOY);

        assertThrows(ReglaNegocioException.class, () -> poliza.renovar(IPC, HOY));
        assertEquals(new BigDecimal("1050000.00"), poliza.getCanonMensual());
    }

    @Test
    void noSePuedeRenovarUnaPolizaCancelada() {
        Poliza poliza = individual();
        poliza.cancelar();
        assertThrows(ReglaNegocioException.class, () -> poliza.renovar(IPC, HOY));
    }

    @Test
    void cancelarPolizaCancelaTodosSusRiesgos() {
        Poliza poliza = colectiva();
        poliza.agregarRiesgo(nuevoRiesgo());
        poliza.agregarRiesgo(nuevoRiesgo());

        poliza.cancelar();

        assertEquals(EstadoPoliza.CANCELADA, poliza.getEstado());
        assertTrue(poliza.getRiesgos().stream().allMatch(Riesgo::estaCancelado));
    }

    @Test
    void noSePuedeCancelarElUnicoRiesgoDeUnaIndividual() {
        Poliza poliza = individual();
        Riesgo unico = poliza.getRiesgos().get(0);
        assertThrows(ReglaNegocioException.class, () -> poliza.cancelarRiesgo(unico));
        assertEquals(1, poliza.riesgosActivos());
    }

    @Test
    void laListaDeRiesgosEsDeSoloLectura() {
        Poliza poliza = colectiva();
        assertThrows(UnsupportedOperationException.class, () -> poliza.getRiesgos().add(nuevoRiesgo()));
    }
}
