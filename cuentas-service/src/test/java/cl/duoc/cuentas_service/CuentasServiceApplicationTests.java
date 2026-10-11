package cl.duoc.cuentas_service;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cl.duoc.bank_common.*;
import cl.duoc.cuentas_service.repository.CuentaRepository;
import cl.duoc.cuentas_service.service.*;
import java.math.BigDecimal;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@AutoConfigureMockMvc
class CuentasServiceApplicationTests {
  @Autowired JdbcTemplate jdbc;
  @Autowired OperacionService operaciones;
  @Autowired CuentaService cuentas;
  @Autowired CuentaRepository repository;
  @Autowired MockMvc mvc;

  @BeforeEach
  void reset() {
    jdbc.update("DELETE FROM historial_eventos");
    jdbc.update("DELETE FROM eventos_pendientes");
    jdbc.update("DELETE FROM operaciones");
    jdbc.update("DELETE FROM cuentas");
    jdbc.update(
        "INSERT INTO cuentas(id,cliente_id,tipo,saldo,estado)"
            + " VALUES(201,101,'corriente',100,'ACTIVA'),(202,102,'ahorro',20,'ACTIVA')");
  }

  private OperacionSolicitud s(String id, String tipo, Long origen, Long destino, String monto) {
    return new OperacionSolicitud(
        id, tipo, origen, destino, new BigDecimal(monto), tipo.equals("PAGO") ? "factura-1" : null);
  }

  @Test
  void transferenciasAtomicasYDuplicados() {
    var request = s("t1", "TRANSFERENCIA", 201L, 202L, "30.00");
    operaciones.procesar(request);
    operaciones.procesar(request);
    assertEquals(new BigDecimal("70.00"), cuentas.buscar(201L).saldo());
    assertEquals(new BigDecimal("50.00"), cuentas.buscar(202L).saldo());
    assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM operaciones", Integer.class));
    assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM eventos_pendientes", Integer.class));
    assertThrows(
        ResponseStatusException.class,
        () -> operaciones.procesar(s("t1", "TRANSFERENCIA", 201L, 202L, "31.00")));
  }

  @Test
  void saldoInsuficienteRevierteTodo() {
    assertThrows(
        ResponseStatusException.class,
        () -> operaciones.procesar(s("t2", "TRANSFERENCIA", 201L, 202L, "101.00")));
    assertEquals(new BigDecimal("100.00"), cuentas.buscar(201L).saldo());
    assertEquals(new BigDecimal("20.00"), cuentas.buscar(202L).saldo());
    assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM operaciones", Integer.class));
    assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM eventos_pendientes", Integer.class));
  }

  @Test
  void depositosPagosRetirosYCierre() {
    operaciones.procesar(s("d1", "DEPOSITO", null, 201L, "20.00"));
    operaciones.procesar(s("p1", "PAGO", 201L, null, "30.00"));
    operaciones.procesar(s("r1", "RETIRO", 201L, null, "90.00"));
    assertEquals(new BigDecimal("0.00"), cuentas.buscar(201L).saldo());
    cuentas.cerrar(201L);
    assertEquals("CERRADA", cuentas.buscar(201L).estado());
    assertThrows(
        ResponseStatusException.class,
        () -> operaciones.procesar(s("d2", "DEPOSITO", null, 201L, "1.00")));
    assertThrows(ResponseStatusException.class, () -> cuentas.cerrar(202L));
  }

  @Test
  void dosRetirosConcurrentesNoSobregiran() throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(2);
    var gate = new CountDownLatch(1);
    try {
      Callable<Boolean> a =
          () -> {
            gate.await();
            try {
              operaciones.procesar(s("rA", "RETIRO", 201L, null, "80.00"));
              return true;
            } catch (ResponseStatusException e) {
              return false;
            }
          };
      Callable<Boolean> b =
          () -> {
            gate.await();
            try {
              operaciones.procesar(s("rB", "RETIRO", 201L, null, "80.00"));
              return true;
            } catch (ResponseStatusException e) {
              return false;
            }
          };
      var fa = pool.submit(a);
      var fb = pool.submit(b);
      gate.countDown();
      assertNotEquals(fa.get(10, TimeUnit.SECONDS), fb.get(10, TimeUnit.SECONDS));
      assertEquals(new BigDecimal("20.00"), cuentas.buscar(201L).saldo());
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void aperturaValidaClienteYPermiteMantenimiento() {
    var builder = RestClient.builder();
    var server = MockRestServiceServer.bindTo(builder).build();
    server
        .expect(requestTo("http://clientes-service/api/clientes/101"))
        .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
    var service = new CuentaService(jdbc, repository, builder, "http://clientes-service");
    var cuenta = service.abrir(101L, "ahorro");
    assertEquals(new BigDecimal("0.00"), cuenta.saldo());
    assertEquals("corriente", service.actualizar(cuenta.id(), "corriente").tipo());
    server.verify();
  }

  @Test
  void seguridadYValidacion() throws Exception {
    mvc.perform(get("/api/cuentas")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/cuentas").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_WEB"))))
        .andExpect(status().isOk());
    mvc.perform(
            post("/api/cuentas")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CAJERO")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/cuentas/operaciones")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CAJERO")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"idempotencia\":\"bad\",\"tipo\":\"RETIRO\",\"cuentaOrigen\":201,\"monto\":-1}"))
        .andExpect(status().isBadRequest());
  }
}
