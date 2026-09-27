package com.ruta.deliverypin.infrastructure.adapter.in.web;

import com.ruta.deliverypin.domain.port.in.GetOperationalCostUseCase;
import com.ruta.deliverypin.domain.port.in.SetSupportHoursUseCase;
import com.ruta.deliverypin.infrastructure.adapter.in.web.dto.OperationalCostResponse;
import com.ruta.deliverypin.infrastructure.adapter.in.web.dto.SetSupportHoursRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

/**
 * Costo por entrega verificada (showback, Fase1 §4.5): informativo, no genera cargos
 * contables. Combina datos que calcula el propio sistema (entregas confirmadas y bytes de
 * evidencia) con tarifas configuradas por variable de entorno y las horas de soporte del mes,
 * que el admin registra porque varian mes a mes.
 */
@Tag(name = "Costo por entrega (showback)", description = "Fase1 §4.5: costo por entrega verificada, calculado con datos reales y tarifas configurables.")
@RestController
@RequestMapping("/api/v1/admin/cost")
public class AdminCostController {

    private final GetOperationalCostUseCase getOperationalCostUseCase;
    private final SetSupportHoursUseCase setSupportHoursUseCase;

    public AdminCostController(
            GetOperationalCostUseCase getOperationalCostUseCase,
            SetSupportHoursUseCase setSupportHoursUseCase
    ) {
        this.getOperationalCostUseCase = getOperationalCostUseCase;
        this.setSupportHoursUseCase = setSupportHoursUseCase;
    }

    @Operation(summary = "Costo por entrega del mes", description = "Showback (Fase1 §4.5): combina entregas confirmadas y GB de evidencia (calculados en SQL) "
            + "con las tarifas configuradas y las horas de soporte de ese mes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Costo calculado para el mes solicitado"),
            @ApiResponse(responseCode = "400", description = "El parametro month no tiene el formato yyyy-MM")
    })
    @GetMapping
    public OperationalCostResponse get(@RequestParam String month) {
        return OperationalCostResponse.from(getOperationalCostUseCase.getCost(parseMonth(month)));
    }

    @Operation(summary = "Registrar horas de soporte del mes", description = "Las horas de soporte varian cada mes y se cargan manualmente; el resto del costo se calcula solo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Horas guardadas; se devuelve el costo recalculado"),
            @ApiResponse(responseCode = "400", description = "El mes no tiene el formato yyyy-MM o las horas son invalidas")
    })
    @PutMapping("/support-hours")
    public OperationalCostResponse setSupportHours(@Valid @RequestBody SetSupportHoursRequest request) {
        YearMonth month = parseMonth(request.month());
        setSupportHoursUseCase.setSupportHours(month, request.hours());
        return OperationalCostResponse.from(getOperationalCostUseCase.getCost(month));
    }

    private YearMonth parseMonth(String month) {
        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("El mes debe tener el formato yyyy-MM, por ejemplo 2026-09.");
        }
    }
}
