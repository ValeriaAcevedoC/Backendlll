package cl.duoc.cuentas_service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CuentasServiceApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {
	}

	@Test
	void rechazaSolicitudesSinToken() throws Exception {
		mockMvc.perform(get("/api/cuentas"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void aceptaSolicitudesConTokenValido() throws Exception {
		mockMvc.perform(get("/api/cuentas").with(jwt()))
				.andExpect(status().isOk());
	}

}
