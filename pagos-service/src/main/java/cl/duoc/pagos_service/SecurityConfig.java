package cl.duoc.pagos_service;

import cl.duoc.bank_common.BancoSecurity;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
  @Bean
  public SecurityFilterChain security(HttpSecurity h) throws Exception {
    h.csrf(c -> c.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers("/actuator/health", "/actuator/health/**")
                    .permitAll()
                    .anyRequest()
                    .hasAnyRole("WEB", "MOVIL", "ADMIN"))
        .oauth2ResourceServer(
            o -> o.jwt(j -> j.jwtAuthenticationConverter(BancoSecurity.converter())));
    return h.build();
  }
}
