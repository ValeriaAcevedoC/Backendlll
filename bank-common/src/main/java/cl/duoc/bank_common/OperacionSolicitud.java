package cl.duoc.bank_common;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record OperacionSolicitud(
    @NotBlank @Size(max = 80) String idempotencia,
    @NotBlank @Pattern(regexp = "DEPOSITO|TRANSFERENCIA|PAGO|RETIRO") String tipo,
    @Positive Long cuentaOrigen,
    @Positive Long cuentaDestino,
    @NotNull @DecimalMin("0.01") @Digits(integer = 13, fraction = 2) BigDecimal monto,
    @Size(max = 200) String referencia) {}
