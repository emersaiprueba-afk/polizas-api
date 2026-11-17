package com.santiagozarate.polizas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Reloj del sistema como dependencia: en las pruebas se reemplaza por una fecha fija. */
@Configuration
public class RelojConfig {

    @Bean
    Clock reloj() {
        return Clock.systemDefaultZone();
    }
}
