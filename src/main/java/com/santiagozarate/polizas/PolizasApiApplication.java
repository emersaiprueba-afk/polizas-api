package com.santiagozarate.polizas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Punto de entrada: arranca Spring Boot y el servidor web embebido (Tomcat). */
@SpringBootApplication
public class PolizasApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PolizasApiApplication.class, args);
    }
}
