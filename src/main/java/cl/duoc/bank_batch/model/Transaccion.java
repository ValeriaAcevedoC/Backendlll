package cl.duoc.bank_batch.model;

import java.math.BigDecimal;

public class Transaccion {

    private Long id;
    private String fecha;
    private BigDecimal monto;
    private String tipo;
    private boolean anomalia;
    private String detalleAnomalia;

    public Transaccion() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public boolean isAnomalia() {
        return anomalia;
    }

    public void setAnomalia(boolean anomalia) {
        this.anomalia = anomalia;
    }

    public String getDetalleAnomalia() {
        return detalleAnomalia;
    }

    public void setDetalleAnomalia(String detalleAnomalia) {
        this.detalleAnomalia = detalleAnomalia;
    }
}