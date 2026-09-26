package com.ruta.deliverypin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DeliveryPinMiddlewareApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeliveryPinMiddlewareApplication.class, args);
    }
}
