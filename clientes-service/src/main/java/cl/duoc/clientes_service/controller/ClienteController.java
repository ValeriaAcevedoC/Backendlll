package cl.duoc.clientes_service.controller;

import cl.duoc.clientes_service.model.Cliente;
import cl.duoc.clientes_service.repository.ClienteRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {
  private final ClienteRepository repository;

  public ClienteController(ClienteRepository r) {
    repository = r;
  }

  @GetMapping
  public List<Cliente> listar() {
    return repository.findAll();
  }

  @GetMapping("/{id}")
  public Cliente buscar(@PathVariable Long id) {
    return repository
        .findById(id)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Cliente crear(@Valid @RequestBody CrearCliente s) {
    return repository.crear(s.nombre(), s.rut(), s.email(), s.perfil());
  }

  @PatchMapping("/{id}")
  public Cliente actualizar(@PathVariable Long id, @Valid @RequestBody ActualizarCliente s) {
    if (s.nombre() != null && s.nombre().isBlank())
      throw new IllegalArgumentException("Nombre vacío");
    if (s.email() != null && s.email().isBlank()) throw new IllegalArgumentException("Email vacío");
    return repository.actualizar(id, s.nombre(), s.email(), s.perfil());
  }

  public record CrearCliente(
      @NotBlank @Size(max = 100) String nombre,
      @NotBlank @Size(max = 20) String rut,
      @NotBlank @Email @Size(max = 150) String email,
      @NotBlank @Pattern(regexp = "PERSONA|EMPRESA") String perfil) {}

  public record ActualizarCliente(
      @Size(max = 100) String nombre,
      @Email @Size(max = 150) String email,
      @Pattern(regexp = "PERSONA|EMPRESA") String perfil) {}
}
