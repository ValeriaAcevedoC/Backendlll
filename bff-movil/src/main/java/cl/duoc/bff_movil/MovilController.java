package cl.duoc.bff_movil;

import cl.duoc.bank_common.*;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bff/movil")
public class MovilController {
  private final CuentasClient cuentas;
  private final CanalClient canal;

  public MovilController(CuentasClient c, CanalClient s) {
    cuentas = c;
    canal = s;
  }

  @GetMapping("/resumen")
  public Map<String, Object> resumen() {
    return Map.of(
        "canal",
        "movil",
        "cuentas",
        cuentas.listar().stream().map(c -> new Saldo(c.id(), c.saldo())).toList());
  }

  @GetMapping("/cuentas/{id}")
  public Resumen cuenta(@PathVariable Long id) {
    var c = cuentas.buscar(id);
    return new Resumen(
        c.id(),
        c.saldo(),
        cuentas.movimientos(id, 0, 5).stream()
            .map(o -> new Movimiento(o.tipo(), o.monto(), o.fecha().toString()))
            .toList());
  }

  @PostMapping("/pagos")
  public OperacionDTO pago(@Valid @RequestBody OperacionSolicitud s) {
    return canal.pagar("", s);
  }

  @PostMapping("/transferencias")
  public OperacionDTO transferencia(@Valid @RequestBody OperacionSolicitud s) {
    return canal.pagar("/transferencias", s);
  }

  public record Saldo(Long cuentaId, BigDecimal saldo) {}

  public record Movimiento(String tipo, BigDecimal monto, String fecha) {}

  public record Resumen(Long cuentaId, BigDecimal saldo, List<Movimiento> ultimosMovimientos) {}
}
