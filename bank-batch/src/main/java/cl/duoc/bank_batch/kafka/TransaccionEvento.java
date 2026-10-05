package cl.duoc.bank_batch.kafka;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransaccionEvento(
        String tipo,
        Long cuentaId,
        BigDecimal monto,
        BigDecimal saldoAnterior,
        BigDecimal saldoPosterior,
        LocalDateTime fecha
) {
}