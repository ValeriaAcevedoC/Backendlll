package cl.duoc.bank_batch;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.batch.job.enabled=false"
})
@AutoConfigureMockMvc
class BankBatchApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void rechazaBffSinToken() throws Exception {
        mockMvc.perform(get("/api/bff/web/resumen"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rechazaBffConRolIncorrecto() throws Exception {
        mockMvc.perform(get("/api/bff/web/resumen")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_MOVIL"))))
                .andExpect(status().isForbidden());
    }
}
