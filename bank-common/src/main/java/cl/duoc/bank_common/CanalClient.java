package cl.duoc.bank_common;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;

public class CanalClient {
  private final RestClient clientes, cuentas, pagos;

  public CanalClient(@Qualifier("remoteRestClientBuilder") RestClient.Builder b) {
    clientes = b.clone().baseUrl("http://clientes-service").build();
    cuentas = b.clone().baseUrl("http://cuentas-service").build();
    pagos = b.clone().baseUrl("http://pagos-service").build();
  }

  @CircuitBreaker(name = "clientesService", fallbackMethod = "fallbackClientes")
  public Object clientes() {
    return clientes.get().uri("/api/clientes").retrieve().body(List.class);
  }

  @CircuitBreaker(name = "pagosService", fallbackMethod = "fallbackPago")
  public OperacionDTO pagar(String ruta, OperacionSolicitud s) {
    return pagos.post().uri("/api/pagos" + ruta).body(s).retrieve().body(OperacionDTO.class);
  }

  @CircuitBreaker(name = "clientesService", fallbackMethod = "fallbackGestion")
  public Object gestionarCliente(HttpMethod metodo, String ruta, Object s) {
    return clientes
        .method(metodo)
        .uri("/api/clientes" + ruta)
        .body(s)
        .retrieve()
        .body(Object.class);
  }

  @CircuitBreaker(name = "cuentasService", fallbackMethod = "fallbackGestion")
  public Object gestionarCuenta(HttpMethod metodo, String ruta, Object s) {
    return cuentas.method(metodo).uri("/api/cuentas" + ruta).body(s).retrieve().body(Object.class);
  }

  public Object fallbackClientes(Throwable e) {
    throw error(e);
  }

  public OperacionDTO fallbackPago(String r, OperacionSolicitud s, Throwable e) {
    throw error(e);
  }

  public Object fallbackGestion(HttpMethod m, String r, Object s, Throwable e) {
    throw error(e);
  }

  private ResponseStatusException error(Throwable e) {
    if (e instanceof HttpClientErrorException h)
      return new ResponseStatusException(
          h.getStatusCode(), "Solicitud rechazada: " + h.getResponseBodyAsString(), e);
    return new ResponseStatusException(
        HttpStatus.SERVICE_UNAVAILABLE,
        "Servicio temporalmente no disponible; conserve la idempotencia al reintentar",
        e);
  }
}
