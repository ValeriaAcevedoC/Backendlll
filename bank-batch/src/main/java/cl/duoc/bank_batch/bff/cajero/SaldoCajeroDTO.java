package cl.duoc.bank_batch.bff.cajero;

import java.math.BigDecimal;

public record SaldoCajeroDTO(
        String canal,
        Long cuentaId,
        BigDecimal saldoDisponible
) {
}