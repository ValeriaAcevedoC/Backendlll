package cl.duoc.bff_web;

import cl.duoc.bank_common.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bff/web")
public class WebController {
  private final CuentasClient cuentas;
  private final CanalClient canal;

  public WebController(CuentasClient c, CanalClient s) {
    cuentas = c;
    canal = s;
  }

  @GetMapping("/resumen")
  public Map<String, Object> resumen() {
    return Map.of("canal", "web", "clientes", canal.clientes(), "cuentas", cuentas.listar());
  }

  @GetMapping("/cuentas/{id}")
  public CuentaDTO cuenta(@PathVariable Long id) {
    return cuentas.buscar(id);
  }

  @GetMapping("/cuentas/{id}/movimientos")
  public List<OperacionDTO> movimientos(
      @PathVariable Long id,
      @RequestParam(defaultValue = "0") int pagina,
      @RequestParam(defaultValue = "20") int limite) {
    return cuentas.movimientos(id, pagina, limite);
  }

  @PostMapping("/clientes")
  @ResponseStatus(HttpStatus.CREATED)
  public Object crearCliente(@RequestBody Map<String, Object> s) {
    return canal.gestionarCliente(HttpMethod.POST, "", s);
  }

  @PatchMapping("/clientes/{id}")
  public Object actualizarCliente(@PathVariable Long id, @RequestBody Map<String, Object> s) {
    return canal.gestionarCliente(HttpMethod.PATCH, "/" + id, s);
  }

  @PostMapping("/cuentas")
  @ResponseStatus(HttpStatus.CREATED)
  public Object abrir(@RequestBody Map<String, Object> s) {
    return canal.gestionarCuenta(HttpMethod.POST, "", s);
  }

  @PatchMapping("/cuentas/{id}")
  public Object mantener(@PathVariable Long id, @RequestBody Map<String, Object> s) {
    return canal.gestionarCuenta(HttpMethod.PATCH, "/" + id, s);
  }

  @PostMapping("/cuentas/{id}/cierre")
  public Object cerrar(@PathVariable Long id) {
    return canal.gestionarCuenta(HttpMethod.POST, "/" + id + "/cierre", Map.of());
  }

  @PostMapping("/pagos")
  public OperacionDTO pago(@Valid @RequestBody OperacionSolicitud s) {
    return canal.pagar("", s);
  }

  @PostMapping("/depositos")
  public OperacionDTO deposito(@Valid @RequestBody OperacionSolicitud s) {
    return canal.pagar("/depositos", s);
  }

  @PostMapping("/transferencias")
  public OperacionDTO transferencia(@Valid @RequestBody OperacionSolicitud s) {
    return canal.pagar("/transferencias", s);
  }
}
