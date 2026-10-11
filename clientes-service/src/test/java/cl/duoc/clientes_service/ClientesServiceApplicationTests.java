package cl.duoc.clientes_service;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ClientesServiceApplicationTests {
  @Autowired MockMvc mvc;

  @Test
  void seguridad() throws Exception {
    mvc.perform(get("/api/clientes")).andExpect(status().isUnauthorized());
    mvc.perform(
            get("/api/clientes").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_WEB"))))
        .andExpect(status().isOk());
    mvc.perform(
            post("/api/clientes")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_MOVIL")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void registroActualizacionYPersistencia() throws Exception {
    String response =
        mvc.perform(
                post("/api/clientes")
                    .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_WEB")))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"nombre\":\"Ana"
                            + " Pérez\",\"rut\":\"20.111.222-3\",\"email\":\"ana@mail.cl\",\"perfil\":\"PERSONA\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    var node = tools.jackson.databind.json.JsonMapper.builder().build().readTree(response);
    long id = node.get("id").asLong();
    mvc.perform(
            patch("/api/clientes/" + id)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_WEB")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"nuevo@mail.cl\",\"perfil\":\"EMPRESA\"}"))
        .andExpect(status().isOk());
    mvc.perform(
            get("/api/clientes/" + id)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_WEB"))))
        .andExpect(jsonPath("$.email").value("nuevo@mail.cl"))
        .andExpect(jsonPath("$.perfil").value("EMPRESA"));
  }

  @Test
  void erroresDeNegocio() throws Exception {
    mvc.perform(
            get("/api/clientes/999999")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_WEB"))))
        .andExpect(status().isNotFound());
    mvc.perform(
            post("/api/clientes")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_WEB")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"nombre\":\"Camila\",\"rut\":\"12.345.678-9\",\"email\":\"camila@mail.cl\",\"perfil\":\"PERSONA\"}"))
        .andExpect(status().isConflict());
    mvc.perform(
            post("/api/clientes")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_WEB")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"nombre\":\"\",\"rut\":\"otro\",\"email\":\"incorrecto\",\"perfil\":\"PERSONA\"}"))
        .andExpect(status().isBadRequest());
  }
}
