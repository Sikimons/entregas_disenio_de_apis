package com.ruta.deliverypin.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SimulateFailuresRequest(
        @NotNull @Min(1) @Max(50) Integer count
) {
}
