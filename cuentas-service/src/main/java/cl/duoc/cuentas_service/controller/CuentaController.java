package cl.duoc.cuentas_service.controller;

import cl.duoc.bank_common.*;
import cl.duoc.cuentas_service.repository.CuentaRepository;
import cl.duoc.cuentas_service.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cuentas")
public class CuentaController {
  private final CuentaRepository repository;
  private final CuentaService service;
  private final OperacionService operaciones;

  public CuentaController(CuentaRepository r, CuentaService s, OperacionService o) {
    repository = r;
    service = s;
    operaciones = o;
  }

  @GetMapping
  public List<CuentaDTO> listar() {
    return repository.findAll();
  }

  @GetMapping("/{id}")
  public CuentaDTO buscar(@PathVariable Long id) {
    return service.buscar(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CuentaDTO abrir(@Valid @RequestBody Apertura s) {
    return service.abrir(s.clienteId(), s.tipo());
  }

  @PatchMapping("/{id}")
  public CuentaDTO actualizar(@PathVariable Long id, @Valid @RequestBody Mantenimiento s) {
    return service.actualizar(id, s.tipo());
  }

  @PostMapping("/{id}/cierre")
  public CuentaDTO cerrar(@PathVariable Long id) {
    return service.cerrar(id);
  }

  @PostMapping("/operaciones")
  public OperacionDTO operar(@Valid @RequestBody OperacionSolicitud s) {
    var auth =
        org.springframework.security.core.context.SecurityContextHolder.getContext()
            .getAuthentication();
    boolean soloCajero =
        auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CAJERO"))
            && auth.getAuthorities().stream()
                .noneMatch(
                    a ->
                        java.util.Set.of("ROLE_WEB", "ROLE_MOVIL", "ROLE_ADMIN")
                            .contains(a.getAuthority()));
    if (soloCajero && !s.tipo().equals("RETIRO"))
      throw new org.springframework.web.server.ResponseStatusException(
          HttpStatus.FORBIDDEN, "El canal cajero solo permite retiros");
    return operaciones.procesar(s);
  }

  @GetMapping("/{id}/movimientos")
  public List<OperacionDTO> movimientos(
      @PathVariable Long id,
      @RequestParam(defaultValue = "0") int pagina,
      @RequestParam(defaultValue = "20") int limite) {
    service.buscar(id);
    if (pagina < 0 || pagina > 100000 || limite < 1 || limite > 100)
      throw new IllegalArgumentException("Paginación inválida");
    return operaciones.movimientos(id, pagina, limite);
  }

  public record Apertura(
      @NotNull @Positive Long clienteId,
      @NotBlank @Pattern(regexp = "corriente|ahorro|prestamo") String tipo) {}

  public record Mantenimiento(
      @NotBlank @Pattern(regexp = "corriente|ahorro|prestamo") String tipo) {}
}
