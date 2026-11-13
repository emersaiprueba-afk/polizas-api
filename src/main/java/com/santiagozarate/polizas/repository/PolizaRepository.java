package com.santiagozarate.polizas.repository;

import com.santiagozarate.polizas.entity.EstadoPoliza;
import com.santiagozarate.polizas.entity.Poliza;
import com.santiagozarate.polizas.entity.TipoPoliza;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Acceso a datos de pólizas. Spring Data genera la implementación:
 * solo declaramos la interfaz.
 */
public interface PolizaRepository extends JpaRepository<Poliza, Long> {

    /** Filtros opcionales: si tipo o estado llegan nulos, no se filtra por ese campo. */
    @Query("""
            SELECT p FROM Poliza p
            WHERE (:tipo IS NULL OR p.tipo = :tipo)
              AND (:estado IS NULL OR p.estado = :estado)
            ORDER BY p.id
            """)
    List<Poliza> buscar(@Param("tipo") TipoPoliza tipo, @Param("estado") EstadoPoliza estado);
}
