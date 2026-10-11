package cl.duoc.bank_common;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CuentaDTO(
    Long id,
    Long clienteId,
    String tipo,
    BigDecimal saldo,
    String estado,
    LocalDateTime fechaApertura) {}
