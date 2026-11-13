package com.santiagozarate.polizas.repository;

import com.santiagozarate.polizas.entity.Riesgo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RiesgoRepository extends JpaRepository<Riesgo, Long> {

    /** Spring Data deduce la consulta a partir del nombre del método. */
    List<Riesgo> findByPolizaIdOrderById(Long polizaId);
}
