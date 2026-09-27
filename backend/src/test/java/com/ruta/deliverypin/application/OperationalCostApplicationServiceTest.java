package com.ruta.deliverypin.application;

import com.ruta.deliverypin.domain.model.CostRates;
import com.ruta.deliverypin.domain.model.DeliveryVolumeSummary;
import com.ruta.deliverypin.domain.model.OperationalCost;
import com.ruta.deliverypin.domain.port.out.CostRatesPort;
import com.ruta.deliverypin.domain.port.out.DeliveryAttemptRepositoryPort;
import com.ruta.deliverypin.domain.port.out.OperationalCostInputPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperationalCostApplicationServiceTest {

    @Mock
    private DeliveryAttemptRepositoryPort deliveryAttemptRepository;
    @Mock
    private OperationalCostInputPort operationalCostInput;
    @Mock
    private CostRatesPort costRatesPort;

    private OperationalCostApplicationService serviceWithMocks() {
        return new OperationalCostApplicationService(deliveryAttemptRepository, operationalCostInput, costRatesPort);
    }

    @Test
    void getCost_withConfirmedDeliveries_computesTotalsAndCostPerDelivery() {
        YearMonth month = YearMonth.of(2026, 9);
        long photoBytes = 2L * 1024 * 1024 * 1024; // 2 GB
        when(deliveryAttemptRepository.summarizeConfirmedBetween(any(Instant.class), any(Instant.class)))
                .thenReturn(new DeliveryVolumeSummary(10, photoBytes));
        when(operationalCostInput.findSupportHours(month)).thenReturn(5.0);
        when(costRatesPort.currentRates()).thenReturn(new CostRates(100.0, 0.5, 20.0));

        OperationalCost cost = serviceWithMocks().getCost(month);

        assertThat(cost.month()).isEqualTo(month);
        assertThat(cost.confirmedDeliveries()).isEqualTo(10);
        assertThat(cost.evidenceStorageGb()).isEqualTo(2.0);
        assertThat(cost.storageCostUsd()).isEqualTo(1.0); // 2 GB * 0.5 USD/GB
        assertThat(cost.supportCostUsd()).isEqualTo(100.0); // 5h * 20 USD/h
        assertThat(cost.totalCostUsd()).isEqualTo(201.0); // 100 infra + 1 storage + 100 support
        assertThat(cost.costPerDeliveryUsd()).isEqualTo(20.1); // 201 / 10
    }

    @Test
    void getCost_withoutConfirmedDeliveries_costPerDeliveryIsNull() {
        YearMonth month = YearMonth.of(2026, 9);
        when(deliveryAttemptRepository.summarizeConfirmedBetween(any(Instant.class), any(Instant.class)))
                .thenReturn(new DeliveryVolumeSummary(0, 0));
        when(operationalCostInput.findSupportHours(month)).thenReturn(0.0);
        when(costRatesPort.currentRates()).thenReturn(new CostRates(50.0, 0.5, 20.0));

        OperationalCost cost = serviceWithMocks().getCost(month);

        assertThat(cost.confirmedDeliveries()).isZero();
        assertThat(cost.totalCostUsd()).isEqualTo(50.0);
        assertThat(cost.costPerDeliveryUsd()).isNull();
    }

    @Test
    void getCost_withZeroRates_totalIsZero() {
        YearMonth month = YearMonth.of(2026, 9);
        when(deliveryAttemptRepository.summarizeConfirmedBetween(any(Instant.class), any(Instant.class)))
                .thenReturn(new DeliveryVolumeSummary(5, 1024L * 1024 * 1024));
        when(operationalCostInput.findSupportHours(month)).thenReturn(10.0);
        when(costRatesPort.currentRates()).thenReturn(new CostRates(0, 0, 0));

        OperationalCost cost = serviceWithMocks().getCost(month);

        assertThat(cost.totalCostUsd()).isZero();
        assertThat(cost.costPerDeliveryUsd()).isZero();
    }

    @Test
    void setSupportHours_negativeHours_throwsWithoutCallingPort() {
        assertThatThrownBy(() -> serviceWithMocks().setSupportHours(YearMonth.of(2026, 9), -1))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(operationalCostInput);
    }

    @Test
    void setSupportHours_validHours_savesThroughPort() {
        YearMonth month = YearMonth.of(2026, 9);

        serviceWithMocks().setSupportHours(month, 8.5);

        verify(operationalCostInput).saveSupportHours(month, 8.5);
    }
}
