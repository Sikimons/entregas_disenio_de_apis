package com.ruta.deliverypin.application;

import com.ruta.deliverypin.domain.model.CostRates;
import com.ruta.deliverypin.domain.model.DeliveryVolumeSummary;
import com.ruta.deliverypin.domain.model.OperationalCost;
import com.ruta.deliverypin.domain.port.in.GetOperationalCostUseCase;
import com.ruta.deliverypin.domain.port.in.SetSupportHoursUseCase;
import com.ruta.deliverypin.domain.port.out.CostRatesPort;
import com.ruta.deliverypin.domain.port.out.DeliveryAttemptRepositoryPort;
import com.ruta.deliverypin.domain.port.out.OperationalCostInputPort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;

/**
 * Costo por entrega verificada (showback, Fase1 §4.5). Es informativo: no genera cargos
 * contables ni cobra a nadie. Combina lo que el propio sistema puede medir (entregas
 * confirmadas y bytes de evidencia) con tarifas externas (infraestructura, almacenamiento,
 * soporte) que no captura la aplicacion.
 */
@Service
public class OperationalCostApplicationService implements GetOperationalCostUseCase, SetSupportHoursUseCase {

    private static final double BYTES_PER_GB = 1024.0 * 1024.0 * 1024.0;

    private final DeliveryAttemptRepositoryPort deliveryAttemptRepository;
    private final OperationalCostInputPort operationalCostInput;
    private final CostRatesPort costRates;

    public OperationalCostApplicationService(
            DeliveryAttemptRepositoryPort deliveryAttemptRepository,
            OperationalCostInputPort operationalCostInput,
            CostRatesPort costRates
    ) {
        this.deliveryAttemptRepository = deliveryAttemptRepository;
        this.operationalCostInput = operationalCostInput;
        this.costRates = costRates;
    }

    @Override
    public OperationalCost getCost(YearMonth month) {
        Instant from = month.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant to = month.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        DeliveryVolumeSummary volume = deliveryAttemptRepository.summarizeConfirmedBetween(from, to);
        double supportHours = operationalCostInput.findSupportHours(month);
        CostRates rates = costRates.currentRates();

        double storageGb = volume.evidencePhotoBytes() / BYTES_PER_GB;
        double storageCost = storageGb * rates.storagePerGbUsd();
        double supportCost = supportHours * rates.supportHourUsd();
        double total = rates.infrastructureMonthlyUsd() + storageCost + supportCost;
        Double costPerDelivery = volume.confirmedDeliveries() > 0 ? total / volume.confirmedDeliveries() : null;

        return new OperationalCost(
                month,
                volume.confirmedDeliveries(),
                volume.evidencePhotoBytes(),
                storageGb,
                rates.infrastructureMonthlyUsd(),
                storageCost,
                supportHours,
                supportCost,
                total,
                costPerDelivery
        );
    }

    @Override
    public void setSupportHours(YearMonth month, double hours) {
        if (hours < 0) {
            throw new IllegalArgumentException("Las horas de soporte no pueden ser negativas.");
        }
        operationalCostInput.saveSupportHours(month, hours);
    }
}
