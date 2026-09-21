package cl.duoc.bank_batch.model;

import java.math.BigDecimal;

public class MovimientoAnual {

    private Long cuentaId;
    private String fecha;
    private String transaccion;
    private BigDecimal monto;
    private String descripcion;

    private boolean anomalia;
    private String detalleAnomalia;

    public MovimientoAnual() {
    }

    public Long getCuentaId() {
        return cuentaId;
    }

    public void setCuentaId(Long cuentaId) {
        this.cuentaId = cuentaId;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getTransaccion() {
        return transaccion;
    }

    public void setTransaccion(String transaccion) {
        this.transaccion = transaccion;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
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