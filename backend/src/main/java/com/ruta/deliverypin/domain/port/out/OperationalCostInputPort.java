package com.ruta.deliverypin.domain.port.out;

import java.time.YearMonth;

/**
 * Puerto de salida para el dato operativo que varia cada mes (horas de soporte) y por eso
 * no se configura como variable de entorno: se guarda y se edita desde el panel administrativo.
 */
public interface OperationalCostInputPort {

    /** 0 si no se ha registrado ningun valor para ese mes. */
    double findSupportHours(YearMonth month);

    void saveSupportHours(YearMonth month, double hours);
}
