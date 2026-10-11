package cl.duoc.bff_cajero;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cl.duoc.bank_common.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@AutoConfigureMockMvc
class CanalTests {
  @Autowired MockMvc mvc;
  @MockitoBean CuentasClient cuentas;
  @MockitoBean CanalClient canal;

  @BeforeEach
  void data() {
    var c =
        new CuentaDTO(201L, 101L, "ahorro", new BigDecimal("100"), "ACTIVA", LocalDateTime.now());
    when(cuentas.listar()).thenReturn(List.of(c));
    when(cuentas.buscar(201L)).thenReturn(c);
    when(canal.clientes()).thenReturn(List.of(Map.of("id", 101)));
  }

  @Test
  void respuestaYRol() throws Exception {
    mvc.perform(get("/api/bff/cajero/saldo/201")).andExpect(status().isUnauthorized());
    mvc.perform(
            get("/api/bff/cajero/saldo/201")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_WEB"))))
        .andExpect(status().isForbidden());
    mvc.perform(
            get("/api/bff/cajero/saldo/201")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CAJERO"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.saldoDisponible").value(100));
  }

  @Test
  void servicioCaidoDevuelve503() throws Exception {
    when(cuentas.buscar(201L))
        .thenThrow(
            new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE, "Servicio temporalmente no disponible"));
    mvc.perform(
            get("/api/bff/cajero/saldo/201")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CAJERO"))))
        .andExpect(status().isServiceUnavailable());
  }

  @Test
  void retiroConservaIdempotencia() throws Exception {
    var op =
        new OperacionDTO(
            "retiro-demo",
            "RETIRO",
            201L,
            null,
            new BigDecimal("20"),
            null,
            new BigDecimal("100"),
            new BigDecimal("80"),
            "CONFIRMADA",
            LocalDateTime.now());
    when(cuentas.operar(any())).thenReturn(op);
    mvc.perform(
            post("/api/bff/cajero/retiro/201")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CAJERO")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotencia\":\"retiro-demo\",\"monto\":20}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.saldoDisponible").value(80));
    verify(cuentas)
        .operar(
            argThat(
                s ->
                    s.idempotencia().equals("retiro-demo")
                        && s.tipo().equals("RETIRO")
                        && s.cuentaOrigen().equals(201L)));
  }
}
