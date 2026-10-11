package cl.duoc.bank_batch;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {"spring.batch.job.enabled=false"})
@AutoConfigureMockMvc
class BankBatchApplicationTests {

  @Autowired private MockMvc mockMvc;
  @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbc;

  @Test
  void contextLoads() {
    org.junit.jupiter.api.Assertions.assertNotNull(
        jdbc.queryForObject("SELECT COUNT(*) FROM BATCH_JOB_INSTANCE", Long.class));
  }

  @Test
  void rechazaBatchSinToken() throws Exception {
    mockMvc.perform(get("/api/batch/ejecuciones/999")).andExpect(status().isUnauthorized());
  }

  @Test
  void rechazaBatchConRolIncorrecto() throws Exception {
    mockMvc
        .perform(
            get("/api/batch/ejecuciones/999")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_MOVIL"))))
        .andExpect(status().isForbidden());
  }
}
