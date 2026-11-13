package com.santiagozarate.polizas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Cuerpo del evento que se envía al CORE: {"evento": "ACTUALIZACION", "polizaId": 555}. */
public record EventoCoreRequest(
        @NotBlank String evento,
        @NotNull Long polizaId
) {
}
