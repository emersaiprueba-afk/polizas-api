package com.santiagozarate.polizas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Datos que el cliente envía para agregar un riesgo. Se validan antes de llegar al servicio. */
public record RiesgoRequest(
        @NotBlank @Size(max = 200) String direccionInmueble,
        @NotBlank @Size(max = 120) String arrendatario,
        @NotBlank @Size(max = 120) String arrendador
) {
}
