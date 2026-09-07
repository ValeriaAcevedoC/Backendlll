package cl.duoc.bank_batch.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    // -------------------------------------------------
    // USUARIOS Y ROLES POR CANAL
    // -------------------------------------------------

    @Bean
    public UserDetailsService userDetailsService() {

        UserDetails usuarioWeb = User.withUsername("web")
                .password("{noop}web123")
                .roles("WEB")
                .build();

        UserDetails usuarioMovil = User.withUsername("movil")
                .password("{noop}movil123")
                .roles("MOVIL")
                .build();

        UserDetails usuarioCajero = User.withUsername("cajero")
                .password("{noop}cajero123")
                .roles("CAJERO")
                .build();

        return new InMemoryUserDetailsManager(
                usuarioWeb,
                usuarioMovil,
                usuarioCajero
        );
    }

    // -------------------------------------------------
    // REGLAS DE SEGURIDAD
    // -------------------------------------------------

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth

                        .requestMatchers("/api/bff/web/**")
                        .hasRole("WEB")

                        .requestMatchers("/api/bff/movil/**")
                        .hasRole("MOVIL")

                        .requestMatchers("/api/bff/cajero/**")
                        .hasRole("CAJERO")

                        .anyRequest()
                        .authenticated()
                )

                .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}