package com.santiagozarate.polizas.entity;

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

import com.santiagozarate.polizas.exception.ReglaNegocioException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Póliza de arrendamiento. Es la raíz del agregado: las reglas que afectan
 * a sus riesgos se ejecutan desde aquí para que nunca queden inconsistentes.
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

    public Poliza(TipoPoliza tipo, String tomador, LocalDate fechaInicio,
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

    // ---------- Reglas de negocio ----------

    /** Prima = canon mensual x número de meses de vigencia. */
    static BigDecimal calcularPrima(BigDecimal canon, int meses) {
        return canon.multiply(BigDecimal.valueOf(meses)).setScale(2, RoundingMode.HALF_UP);
    }

    public boolean estaCancelada() {
        return estado == EstadoPoliza.CANCELADA;
    }

    public long riesgosActivos() {
        return riesgos.stream().filter(r -> r.getEstado() == EstadoRiesgo.ACTIVO).count();
    }

    /**
     * Agrega un riesgo validando el tipo de póliza.
     * Una individual solo admite un riesgo activo; una colectiva admite varios.
     */
    public void agregarRiesgo(Riesgo riesgo) {
        if (estaCancelada()) {
            throw new ReglaNegocioException("No se pueden agregar riesgos a una póliza cancelada");
        }
        if (tipo == TipoPoliza.INDIVIDUAL && riesgosActivos() >= 1) {
            throw new ReglaNegocioException("Una póliza individual solo puede tener 1 riesgo");
        }
        riesgo.asignarPoliza(this);
        riesgos.add(riesgo);
    }

    /** Renueva por el mismo periodo inicial y ajusta canon y prima según el IPC. */
    public void renovar(BigDecimal ipc) {
        if (estaCancelada()) {
            throw new ReglaNegocioException("No se puede renovar una póliza cancelada");
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

    // ---------- Getters (sin setters públicos: el estado solo cambia por las reglas) ----------

    public Long getId() { return id; }
    public TipoPoliza getTipo() { return tipo; }
    public EstadoPoliza getEstado() { return estado; }
    public String getTomador() { return tomador; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public LocalDate getFechaFin() { return fechaFin; }
    public int getMesesVigencia() { return mesesVigencia; }
    public BigDecimal getCanonMensual() { return canonMensual; }
    public BigDecimal getPrima() { return prima; }
    public List<Riesgo> getRiesgos() { return riesgos; }
}
