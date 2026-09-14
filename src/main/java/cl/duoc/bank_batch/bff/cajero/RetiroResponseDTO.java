package cl.duoc.bank_batch.bff.cajero;

import java.math.BigDecimal;

public record RetiroResponseDTO(
        String canal,
        Long cuentaId,
        BigDecimal montoRetirado,
        BigDecimal saldoAnterior,
        BigDecimal saldoDisponible
) {
}