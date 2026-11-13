package com.santiagozarate.polizas.config;

import com.santiagozarate.polizas.entity.Poliza;
import com.santiagozarate.polizas.entity.Riesgo;
import com.santiagozarate.polizas.entity.TipoPoliza;
import com.santiagozarate.polizas.repository.PolizaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Carga pólizas de ejemplo al arrancar, para probar la API de inmediato.
 * Solo corre en el perfil "local" (no en pruebas ni en producción).
 */
@Configuration
@Profile("local")
public class DatosIniciales {

    @Bean
    CommandLineRunner cargarDatos(PolizaRepository repo) {
        return args -> {
            Poliza individual = new Poliza(TipoPoliza.INDIVIDUAL, "Laura Gómez",
                    LocalDate.of(2026, 1, 1), 12, new BigDecimal("1500000"));
            individual.agregarRiesgo(new Riesgo("Cra 7 # 72-10 Apto 301", "Laura Gómez", "Pedro Ruiz"));

            Poliza colectiva = new Poliza(TipoPoliza.COLECTIVA, "Inmobiliaria Los Andes S.A.S.",
                    LocalDate.of(2026, 3, 1), 12, new BigDecimal("2000000"));
            colectiva.agregarRiesgo(new Riesgo("Cl 100 # 15-20 Apto 502", "Carlos Díaz", "Ana Torres"));
            colectiva.agregarRiesgo(new Riesgo("Cl 85 # 11-40 Apto 201", "Marta León", "Jorge Silva"));

            repo.save(individual);
            repo.save(colectiva);
        };
    }
}
