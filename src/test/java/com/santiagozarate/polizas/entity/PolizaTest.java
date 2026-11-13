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

    private Poliza nuevaPoliza(TipoPoliza tipo) {
        return new Poliza(tipo, "Tomador", LocalDate.of(2026, 1, 1), 12, new BigDecimal("1000000"));
    }

    private Riesgo nuevoRiesgo() {
        return new Riesgo("Cra 1 # 2-3", "Arrendatario", "Arrendador");
    }

    @Test
    void primaEsCanonPorMesesDeVigencia() {
        Poliza poliza = nuevaPoliza(TipoPoliza.INDIVIDUAL);
        assertEquals(new BigDecimal("12000000.00"), poliza.getPrima());
    }

    @Test
    void individualSoloAdmiteUnRiesgo() {
        Poliza poliza = nuevaPoliza(TipoPoliza.INDIVIDUAL);
        poliza.agregarRiesgo(nuevoRiesgo());
        assertThrows(ReglaNegocioException.class, () -> poliza.agregarRiesgo(nuevoRiesgo()));
    }

    @Test
    void colectivaAdmiteVariosRiesgos() {
        Poliza poliza = nuevaPoliza(TipoPoliza.COLECTIVA);
        poliza.agregarRiesgo(nuevoRiesgo());
        poliza.agregarRiesgo(nuevoRiesgo());
        assertEquals(2, poliza.riesgosActivos());
    }

    @Test
    void renovarAjustaCanonYPrimaPorIpcYCambiaEstado() {
        Poliza poliza = nuevaPoliza(TipoPoliza.INDIVIDUAL);
        poliza.renovar(new BigDecimal("0.05"));

        assertEquals(new BigDecimal("1050000.00"), poliza.getCanonMensual());
        assertEquals(new BigDecimal("12600000.00"), poliza.getPrima());
        assertEquals(EstadoPoliza.RENOVADA, poliza.getEstado());
        assertEquals(LocalDate.of(2027, 1, 1), poliza.getFechaInicio());
        assertEquals(LocalDate.of(2028, 1, 1), poliza.getFechaFin());
    }

    @Test
    void noSePuedeRenovarUnaPolizaCancelada() {
        Poliza poliza = nuevaPoliza(TipoPoliza.INDIVIDUAL);
        poliza.cancelar();
        assertThrows(ReglaNegocioException.class, () -> poliza.renovar(new BigDecimal("0.05")));
    }

    @Test
    void cancelarPolizaCancelaTodosSusRiesgos() {
        Poliza poliza = nuevaPoliza(TipoPoliza.COLECTIVA);
        poliza.agregarRiesgo(nuevoRiesgo());
        poliza.agregarRiesgo(nuevoRiesgo());

        poliza.cancelar();

        assertEquals(EstadoPoliza.CANCELADA, poliza.getEstado());
        assertTrue(poliza.getRiesgos().stream().allMatch(Riesgo::estaCancelado));
    }
}
