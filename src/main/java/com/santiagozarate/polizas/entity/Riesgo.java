package com.santiagozarate.polizas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Un riesgo es un inmueble arrendado y asegurado dentro de una póliza. */
@Entity
@Table(name = "riesgos")
public class Riesgo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String direccionInmueble;

    /** Arrendatario: es el asegurado. */
    @Column(nullable = false)
    private String arrendatario;

    /** Arrendador: es el beneficiario en caso de siniestro. */
    @Column(nullable = false)
    private String arrendador;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoRiesgo estado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "poliza_id")
    private Poliza poliza;

    protected Riesgo() {
    }

    public Riesgo(String direccionInmueble, String arrendatario, String arrendador) {
        this.direccionInmueble = direccionInmueble;
        this.arrendatario = arrendatario;
        this.arrendador = arrendador;
        this.estado = EstadoRiesgo.ACTIVO;
    }

    /** Solo la póliza llama este método, al agregar el riesgo. */
    void asignarPoliza(Poliza poliza) {
        this.poliza = poliza;
    }

    /** Solo la póliza puede cancelar un riesgo (método de paquete). */
    void cancelar() {
        this.estado = EstadoRiesgo.CANCELADO;
    }

    public boolean estaCancelado() {
        return estado == EstadoRiesgo.CANCELADO;
    }

    public Long getId() { return id; }
    public String getDireccionInmueble() { return direccionInmueble; }
    public String getArrendatario() { return arrendatario; }
    public String getArrendador() { return arrendador; }
    public EstadoRiesgo getEstado() { return estado; }
    public Poliza getPoliza() { return poliza; }
}
