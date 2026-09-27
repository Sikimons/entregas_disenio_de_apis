package com.ruta.deliverypin.domain.port.in;

import java.time.YearMonth;

/** Registra las horas de soporte tecnico dedicadas a la plataforma en un mes dado. */
public interface SetSupportHoursUseCase {

    void setSupportHours(YearMonth month, double hours);
}
