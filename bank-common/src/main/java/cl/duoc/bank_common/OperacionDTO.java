package cl.duoc.bank_common;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OperacionDTO(
    String idempotencia,
    String tipo,
    Long cuentaOrigen,
    Long cuentaDestino,
    BigDecimal monto,
    String referencia,
    BigDecimal saldoAnterior,
    BigDecimal saldoPosterior,
    String estado,
    LocalDateTime fecha) {}
