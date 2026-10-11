package cl.duoc.pagos_service;

import cl.duoc.bank_common.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {
  private final CuentasClient cuentas;

  public PagoController(CuentasClient c) {
    cuentas = c;
  }

  @PostMapping
  public OperacionDTO pago(@Valid @RequestBody OperacionSolicitud s) {
    return operar("PAGO", s);
  }

  @PostMapping("/depositos")
  public OperacionDTO deposito(@Valid @RequestBody OperacionSolicitud s) {
    return operar("DEPOSITO", s);
  }

  @PostMapping("/transferencias")
  public OperacionDTO transferencia(@Valid @RequestBody OperacionSolicitud s) {
    return operar("TRANSFERENCIA", s);
  }

  private OperacionDTO operar(String tipo, OperacionSolicitud s) {
    if (!tipo.equals(s.tipo())) throw new IllegalArgumentException("El tipo debe ser " + tipo);
    return cuentas.operar(s);
  }
}
