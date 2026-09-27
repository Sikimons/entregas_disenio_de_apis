package com.ruta.deliverypin.domain.port.in;

import com.ruta.deliverypin.domain.model.OperationalCost;

import java.time.YearMonth;

/** Calcula el costo por entrega verificada de un mes (showback, Fase1 §4.5). */
public interface GetOperationalCostUseCase {

    OperationalCost getCost(YearMonth month);
}
