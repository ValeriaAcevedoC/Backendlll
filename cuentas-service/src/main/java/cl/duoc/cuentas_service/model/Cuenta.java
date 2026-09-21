package cl.duoc.cuentas_service.model;

import java.math.BigDecimal;

public class Cuenta {
    private Long id;
    private Long clienteId;
    private String tipo;
    private BigDecimal saldo;

    public Cuenta(Long id, Long clienteId, String tipo, BigDecimal saldo) {
        this.id = id;
        this.clienteId = clienteId;
        this.tipo = tipo;
        this.saldo = saldo;
    }

    public Long getId() { return id; }
    public Long getClienteId() { return clienteId; }
    public String getTipo() { return tipo; }
    public BigDecimal getSaldo() { return saldo; }
}