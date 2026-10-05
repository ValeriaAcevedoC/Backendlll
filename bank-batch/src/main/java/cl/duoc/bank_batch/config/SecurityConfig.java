package cl.duoc.bank_batch.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.core.convert.converter.Converter;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth

                        // Solo el endpoint de salud es publico. Las rutas de
                        // negocio y diagnostico requieren un token OAuth2.
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/health/**"
                        )
                        .permitAll()

                        // BFF WEB
                        .requestMatchers("/api/bff/web/**")
                        .hasRole("WEB")

                        // BFF MOVIL
                        .requestMatchers("/api/bff/movil/**")
                        .hasRole("MOVIL")

                        // BFF CAJERO
                        .requestMatchers("/api/bff/cajero/**")
                        .hasRole("CAJERO")

                        // CUALQUIER OTRA RUTA REQUIERE AUTENTICACION
                        .anyRequest()
                        .authenticated()
                )

                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt ->
                                jwt.jwtAuthenticationConverter(
                                        jwtAuthenticationConverter()
                                )
                        )
                );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {

        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(
                keycloakRealmRoleConverter()
        );

        return converter;
    }

    private Converter<Jwt, Collection<GrantedAuthority>>
    keycloakRealmRoleConverter() {

        return jwt -> {

            Collection<GrantedAuthority> authorities =
                    new ArrayList<>();

            Map<String, Object> realmAccess =
                    jwt.getClaim("realm_access");

            if (realmAccess == null) {
                return authorities;
            }

            Object rolesObject =
                    realmAccess.get("roles");

            if (!(rolesObject instanceof List<?> roles)) {
                return authorities;
            }

            for (Object role : roles) {

                authorities.add(
                        new SimpleGrantedAuthority(
                                "ROLE_" + role.toString()
                        )
                );
            }

            return authorities;
        };
    }
}
