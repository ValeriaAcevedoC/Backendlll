package cl.duoc.cuentas_service.service;

import cl.duoc.bank_common.*;
import cl.duoc.cuentas_service.repository.CuentaRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CuentaService {
  private final JdbcTemplate jdbc;
  private final CuentaRepository repository;
  private final RestClient clientes;

  public CuentaService(
      JdbcTemplate j,
      CuentaRepository r,
      @Qualifier("remoteRestClientBuilder") RestClient.Builder b,
      @Value("${services.clientes.base-url:http://clientes-service}") String url) {
    jdbc = j;
    repository = r;
    clientes = b.baseUrl(url).build();
  }

  public CuentaDTO buscar(Long id) {
    return repository
        .findById(id)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta no encontrada"));
  }

  @CircuitBreaker(name = "clientesService", fallbackMethod = "falloCliente")
  public CuentaDTO abrir(Long clienteId, String tipo) {
    clientes.get().uri("/api/clientes/{id}", clienteId).retrieve().toBodilessEntity();
    var key = new org.springframework.jdbc.support.GeneratedKeyHolder();
    jdbc.update(
        c -> {
          var s =
              c.prepareStatement(
                  "INSERT INTO cuentas(cliente_id,tipo,saldo,estado) VALUES(?,?,0,'ACTIVA')",
                  new String[] {"id"});
          s.setLong(1, clienteId);
          s.setString(2, tipo);
          return s;
        },
        key);
    return buscar(key.getKey().longValue());
  }

  public CuentaDTO falloCliente(Long clienteId, String tipo, Throwable e) {
    if (e instanceof HttpClientErrorException h)
      throw new ResponseStatusException(
          h.getStatusCode(), "Cliente inválido o acceso rechazado", e);
    throw new ResponseStatusException(
        HttpStatus.SERVICE_UNAVAILABLE, "No se pudo verificar el cliente", e);
  }

  @Transactional
  public CuentaDTO actualizar(Long id, String tipo) {
    var c = lock(id);
    if (!c.estado().equals("ACTIVA"))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Cuenta cerrada");
    jdbc.update("UPDATE cuentas SET tipo=? WHERE id=?", tipo, id);
    return buscar(id);
  }

  @Transactional
  public CuentaDTO cerrar(Long id) {
    var c = lock(id);
    if (c.saldo().signum() != 0)
      throw new ResponseStatusException(HttpStatus.CONFLICT, "La cuenta debe tener saldo cero");
    jdbc.update("UPDATE cuentas SET estado='CERRADA' WHERE id=?", id);
    return buscar(id);
  }

  private CuentaDTO lock(Long id) {
    var rows = jdbc.query("SELECT * FROM cuentas WHERE id=? FOR UPDATE", repository::map, id);
    if (rows.isEmpty())
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta no encontrada");
    return rows.getFirst();
  }
}
