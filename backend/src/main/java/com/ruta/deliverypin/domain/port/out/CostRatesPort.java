package com.ruta.deliverypin.domain.port.out;

import com.ruta.deliverypin.domain.model.CostRates;

/** Puerto de salida para las tarifas de costo (configuradas por variable de entorno). */
public interface CostRatesPort {

    CostRates currentRates();
}
