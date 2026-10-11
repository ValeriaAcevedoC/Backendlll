package cl.duoc.pagos_service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cl.duoc.bank_common.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PagosTests {
  @Autowired MockMvc mvc;
  @MockitoBean CuentasClient cuentas;

  @Test
  void derivaTransferencia() throws Exception {
    mvc.perform(
            post("/api/pagos/transferencias")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_WEB")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"idempotencia\":\"transferencia-demo\",\"tipo\":\"TRANSFERENCIA\",\"cuentaOrigen\":201,\"cuentaDestino\":202,\"monto\":25}"))
        .andExpect(status().isOk());
    verify(cuentas)
        .operar(argThat(s -> s.tipo().equals("TRANSFERENCIA") && s.cuentaDestino().equals(202L)));
  }

  @Test
  void validaTipoMontoYRol() throws Exception {
    mvc.perform(post("/api/pagos").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            post("/api/pagos")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CAJERO")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/pagos/depositos")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_WEB")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotencia\":\"t\",\"tipo\":\"TRANSFERENCIA\",\"monto\":25}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/api/pagos")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_WEB")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotencia\":\"p\",\"tipo\":\"PAGO\",\"monto\":0}"))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(cuentas);
  }
}
