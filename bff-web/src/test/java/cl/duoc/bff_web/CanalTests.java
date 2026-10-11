package cl.duoc.bff_web;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cl.duoc.bank_common.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.*;
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
    mvc.perform(get("/api/bff/web/resumen")).andExpect(status().isUnauthorized());
    mvc.perform(
            get("/api/bff/web/resumen")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_MOVIL"))))
        .andExpect(status().isForbidden());
    mvc.perform(
            get("/api/bff/web/resumen")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_WEB"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.clientes").exists())
        .andExpect(jsonPath("$.cuentas[0].clienteId").value(101));
  }

  @Test
  void servicioCaidoDevuelve503() throws Exception {
    when(cuentas.listar())
        .thenThrow(
            new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE, "Servicio temporalmente no disponible"));
    mvc.perform(
            get("/api/bff/web/resumen")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_WEB"))))
        .andExpect(status().isServiceUnavailable());
  }
}
