package com.santiagozarate.polizas.dto;

import com.santiagozarate.polizas.entity.EstadoPoliza;
import com.santiagozarate.polizas.entity.Poliza;
import com.santiagozarate.polizas.entity.TipoPoliza;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Lo que la API devuelve de una póliza. Usamos un DTO (y no la entidad)
 * para no exponer la estructura interna de la base de datos.
 */
public record PolizaResponse(
        Long id,
        TipoPoliza tipo,
        EstadoPoliza estado,
        String tomador,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        int mesesVigencia,
        BigDecimal canonMensual,
        BigDecimal prima,
        long riesgosActivos
) {
    public static PolizaResponse desde(Poliza p) {
        return new PolizaResponse(
                p.getId(), p.getTipo(), p.getEstado(), p.getTomador(),
                p.getFechaInicio(), p.getFechaFin(), p.getMesesVigencia(),
                p.getCanonMensual(), p.getPrima(), p.riesgosActivos());
    }
}
