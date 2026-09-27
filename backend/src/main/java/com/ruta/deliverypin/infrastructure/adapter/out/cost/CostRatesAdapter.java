package com.ruta.deliverypin.infrastructure.adapter.out.cost;

import com.ruta.deliverypin.domain.model.CostRates;
import com.ruta.deliverypin.domain.port.out.CostRatesPort;
import com.ruta.deliverypin.infrastructure.config.CostProperties;
import org.springframework.stereotype.Component;

/** Adaptador de salida: expone las tarifas configuradas por variable de entorno como CostRates. */
@Component
public class CostRatesAdapter implements CostRatesPort {

    private final CostProperties costProperties;

    public CostRatesAdapter(CostProperties costProperties) {
        this.costProperties = costProperties;
    }

    @Override
    public CostRates currentRates() {
        return new CostRates(
                costProperties.getInfraMonthlyUsd(),
                costProperties.getStoragePerGbUsd(),
                costProperties.getSupportHourUsd()
        );
    }
}
