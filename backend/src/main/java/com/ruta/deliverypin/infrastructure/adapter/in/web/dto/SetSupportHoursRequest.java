package com.ruta.deliverypin.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record SetSupportHoursRequest(
        @NotNull @Pattern(regexp = "\\d{4}-\\d{2}", message = "El mes debe tener el formato yyyy-MM") String month,
        @NotNull @DecimalMin(value = "0", message = "Las horas de soporte no pueden ser negativas") Double hours
) {
}
