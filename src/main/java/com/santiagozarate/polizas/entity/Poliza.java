package com.santiagozarate.polizas.entity;

import com.santiagozarate.polizas.exception.ReglaNegocioException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Póliza de arrendamiento. Es la raíz del agregado: todo cambio sobre la
 * póliza o sus riesgos pasa por esta clase, que valida las reglas de negocio.
 */
@Entity
@Table(name = "polizas")
public class Poliza {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoPoliza tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPoliza estado;

    /** Inmobiliaria o administración (colectiva) o arrendatario (individual). */
    @Column(nullable = false)
    private String tomador;

    @Column(nullable = false)
    private LocalDate fechaInicio;

    @Column(nullable = false)
    private LocalDate fechaFin;

    @Column(nullable = false)
    private int mesesVigencia;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal canonMensual;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal prima;

    /** Control de concurrencia optimista: evita que dos usuarios se pisen cambios. */
    @Version
    private Long version;

    @OneToMany(mappedBy = "poliza", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Riesgo> riesgos = new ArrayList<>();

    /** Constructor vacío: JPA lo exige para crear objetos al leer de la base de datos. */
    protected Poliza() {
    }

    private Poliza(TipoPoliza tipo, String tomador, LocalDate fechaInicio,
                   int mesesVigencia, BigDecimal canonMensual) {
        this.tipo = tipo;
        this.tomador = tomador;
        this.fechaInicio = fechaInicio;
        this.mesesVigencia = mesesVigencia;
        this.fechaFin = fechaInicio.plusMonths(mesesVigencia);
        this.canonMensual = canonMensual.setScale(2, RoundingMode.HALF_UP);
        this.prima = calcularPrima(this.canonMensual, mesesVigencia);
        this.estado = EstadoPoliza.ACTIVA;
    }

    // ---------- Creación: cada tipo tiene su propia forma de nacer ----------

    /** Una póliza individual nace con su único riesgo; no se le pueden agregar más. */
    public static Poliza individual(String tomador, LocalDate fechaInicio, int mesesVigencia,
                                    BigDecimal canonMensual, Riesgo riesgo) {
        Poliza poliza = new Poliza(TipoPoliza.INDIVIDUAL, tomador, fechaInicio, mesesVigencia, canonMensual);
        riesgo.asignarPoliza(poliza);
        poliza.riesgos.add(riesgo);
        return poliza;
    }

    /** Una póliza colectiva nace vacía y se le agregan riesgos con agregarRiesgo(). */
    public static Poliza colectiva(String tomador, LocalDate fechaInicio, int mesesVigencia,
                                   BigDecimal canonMensual) {
        return new Poliza(TipoPoliza.COLECTIVA, tomador, fechaInicio, mesesVigencia, canonMensual);
    }

    // ---------- Reglas de negocio ----------

    /** Prima = canon mensual x número de meses de vigencia. */
    static BigDecimal calcularPrima(BigDecimal canon, int meses) {
        return canon.multiply(BigDecimal.valueOf(meses)).setScale(2, RoundingMode.HALF_UP);
    }

    public boolean estaCancelada() {
        return estado == EstadoPoliza.CANCELADA;
    }

    public long riesgosActivos() {
        return riesgos.stream().filter(r -> !r.estaCancelado()).count();
    }

    /** Solo las pólizas colectivas admiten riesgos adicionales. */
    public void agregarRiesgo(Riesgo riesgo) {
        if (estaCancelada()) {
            throw new ReglaNegocioException("No se pueden agregar riesgos a una póliza cancelada");
        }
        if (tipo != TipoPoliza.COLECTIVA) {
            throw new ReglaNegocioException("Solo se pueden agregar riesgos a pólizas colectivas");
        }
        riesgo.asignarPoliza(this);
        riesgos.add(riesgo);
    }

    /**
     * Renueva por el mismo periodo inicial y ajusta canon y prima según el IPC.
     * Es idempotente: si la vigencia renovada todavía no ha empezado, la póliza
     * ya fue renovada y un segundo intento (por ejemplo, un doble clic) se rechaza.
     */
    public void renovar(BigDecimal ipc, LocalDate hoy) {
        if (estaCancelada()) {
            throw new ReglaNegocioException("No se puede renovar una póliza cancelada");
        }
        if (fechaInicio.isAfter(hoy)) {
            throw new ReglaNegocioException(
                    "La póliza ya fue renovada para el periodo que inicia el " + fechaInicio);
        }
        BigDecimal factor = BigDecimal.ONE.add(ipc);
        this.canonMensual = canonMensual.multiply(factor).setScale(2, RoundingMode.HALF_UP);
        this.prima = calcularPrima(canonMensual, mesesVigencia);
        this.fechaInicio = fechaFin;
        this.fechaFin = fechaFin.plusMonths(mesesVigencia);
        this.estado = EstadoPoliza.RENOVADA;
    }

    /** Cancelar la póliza cancela todos sus riesgos. */
    public void cancelar() {
        if (estaCancelada()) {
            throw new ReglaNegocioException("La póliza ya está cancelada");
        }
        this.estado = EstadoPoliza.CANCELADA;
        riesgos.forEach(Riesgo::cancelar);
    }

    /**
     * Cancela un riesgo de esta póliza. En una individual no se permite cancelar
     * su único riesgo, porque la póliza quedaría activa sin nada asegurado:
     * en ese caso lo que corresponde es cancelar la póliza.
     */
    public void cancelarRiesgo(Riesgo riesgo) {
        if (!riesgos.contains(riesgo)) {
            throw new ReglaNegocioException("El riesgo no pertenece a esta póliza");
        }
        if (riesgo.estaCancelado()) {
            throw new ReglaNegocioException("El riesgo ya está cancelado");
        }
        if (tipo == TipoPoliza.INDIVIDUAL) {
            throw new ReglaNegocioException(
                    "No se puede cancelar el único riesgo de una póliza individual; cancele la póliza");
        }
        riesgo.cancelar();
    }

    // ---------- Getters (sin setters: el estado solo cambia por las reglas) ----------

    public Long getId() { return id; }
    public TipoPoliza getTipo() { return tipo; }
    public EstadoPoliza getEstado() { return estado; }
    public String getTomador() { return tomador; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public LocalDate getFechaFin() { return fechaFin; }
    public int getMesesVigencia() { return mesesVigencia; }
    public BigDecimal getCanonMensual() { return canonMensual; }
    public BigDecimal getPrima() { return prima; }

    /** Lista de solo lectura: los riesgos solo cambian a través de los métodos de la póliza. */
    public List<Riesgo> getRiesgos() { return Collections.unmodifiableList(riesgos); }
}
