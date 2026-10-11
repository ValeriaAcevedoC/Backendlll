package cl.duoc.cuentas_service.config;

import cl.duoc.bank_common.*;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Import(RemoteServices.class)
public class SecurityConfig {
  @Bean
  public SecurityFilterChain filterChain(HttpSecurity h) throws Exception {
    h.csrf(c -> c.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers("/actuator/health", "/actuator/health/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/cuentas/operaciones")
                    .hasAnyRole("WEB", "MOVIL", "CAJERO", "ADMIN")
                    .requestMatchers(HttpMethod.GET, "/api/**")
                    .hasAnyRole("WEB", "MOVIL", "CAJERO", "ADMIN")
                    .anyRequest()
                    .hasAnyRole("WEB", "ADMIN"))
        .oauth2ResourceServer(
            o -> o.jwt(j -> j.jwtAuthenticationConverter(BancoSecurity.converter())));
    return h.build();
  }
}
