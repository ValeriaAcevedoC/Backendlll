package cl.duoc.bank_common;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import org.springframework.beans.factory.annotation.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;

public class CuentasClient {
  private final RestClient client;

  public CuentasClient(
      @Qualifier("remoteRestClientBuilder") RestClient.Builder b,
      @Value("${services.cuentas.base-url:http://cuentas-service}") String url) {
    client = b.baseUrl(url).build();
  }

  @CircuitBreaker(name = "cuentasService", fallbackMethod = "fallbackLista")
  public List<CuentaDTO> listar() {
    return client.get().uri("/api/cuentas").retrieve().body(new ParameterizedTypeReference<>() {});
  }

  @CircuitBreaker(name = "cuentasService", fallbackMethod = "fallbackCuenta")
  public CuentaDTO buscar(Long id) {
    return client.get().uri("/api/cuentas/{id}", id).retrieve().body(CuentaDTO.class);
  }

  @CircuitBreaker(name = "cuentasService", fallbackMethod = "fallbackOperacion")
  public OperacionDTO operar(OperacionSolicitud s) {
    return client
        .post()
        .uri("/api/cuentas/operaciones")
        .body(s)
        .retrieve()
        .body(OperacionDTO.class);
  }

  @CircuitBreaker(name = "cuentasService", fallbackMethod = "fallbackMovimientos")
  public List<OperacionDTO> movimientos(Long id, int pagina, int limite) {
    return client
        .get()
        .uri("/api/cuentas/{id}/movimientos?pagina={p}&limite={l}", id, pagina, limite)
        .retrieve()
        .body(new ParameterizedTypeReference<>() {});
  }

  public List<CuentaDTO> fallbackLista(Throwable e) {
    throw unavailable(e);
  }

  public CuentaDTO fallbackCuenta(Long id, Throwable e) {
    throw unavailable(e);
  }

  public OperacionDTO fallbackOperacion(OperacionSolicitud s, Throwable e) {
    throw unavailable(e);
  }

  public List<OperacionDTO> fallbackMovimientos(Long id, int p, int l, Throwable e) {
    throw unavailable(e);
  }

  private ResponseStatusException unavailable(Throwable e) {
    if (e instanceof HttpClientErrorException h)
      return new ResponseStatusException(
          h.getStatusCode(), "Operación rechazada por cuentas: " + h.getResponseBodyAsString(), e);
    return new ResponseStatusException(
        HttpStatus.SERVICE_UNAVAILABLE,
        "Cuentas temporalmente no disponible; reintente con la misma idempotencia",
        e);
  }
}
