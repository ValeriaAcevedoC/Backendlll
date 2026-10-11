package cl.duoc.bff_cajero;

import cl.duoc.bank_common.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bff/cajero")
public class CajeroController {
  private final CuentasClient cuentas;

  public CajeroController(CuentasClient c) {
    cuentas = c;
  }

  @GetMapping("/saldo/{id}")
  public Saldo saldo(@PathVariable Long id) {
    var c = cuentas.buscar(id);
    return new Saldo("cajero", id, c.saldo());
  }

  @PostMapping("/retiro/{id}")
  public Retiro retiro(@PathVariable Long id, @Valid @RequestBody Solicitud s) {
    var o =
        cuentas.operar(
            new OperacionSolicitud(s.idempotencia(), "RETIRO", id, null, s.monto(), null));
    return new Retiro(
        "cajero", id, o.monto(), o.saldoAnterior(), o.saldoPosterior(), o.idempotencia());
  }

  public record Solicitud(
      @NotBlank @Size(max = 80) String idempotencia,
      @NotNull @DecimalMin("0.01") @Digits(integer = 13, fraction = 2) BigDecimal monto) {}

  public record Saldo(String canal, Long cuentaId, BigDecimal saldoDisponible) {}

  public record Retiro(
      String canal,
      Long cuentaId,
      BigDecimal montoRetirado,
      BigDecimal saldoAnterior,
      BigDecimal saldoDisponible,
      String idempotencia) {}
}
