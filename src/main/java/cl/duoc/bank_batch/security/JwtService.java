package cl.duoc.bank_batch.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {

    private static final String CLAVE_SECRETA =
            "BancoXYZ-Semana5-ClaveSegura-JWT-2026-BackendIII";

    private static final long DURACION_TOKEN =
            60 * 60 * 1000; // 1 hora

    private SecretKey obtenerClave() {
        return Keys.hmacShaKeyFor(
                CLAVE_SECRETA.getBytes(StandardCharsets.UTF_8)
        );
    }

    public String generarToken(
            String usuario,
            List<String> roles) {

        Date ahora = new Date();

        Date expiracion = new Date(
                ahora.getTime() + DURACION_TOKEN
        );

        return Jwts.builder()
                .subject(usuario)
                .claim("roles", roles)
                .issuedAt(ahora)
                .expiration(expiracion)
                .signWith(obtenerClave())
                .compact();
    }

    public String obtenerUsuario(String token) {

        return obtenerClaims(token)
                .getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> obtenerRoles(String token) {

        return obtenerClaims(token)
                .get("roles", List.class);
    }

    public boolean esTokenValido(String token) {

        try {
            obtenerClaims(token);
            return true;

        } catch (Exception e) {
            return false;
        }
    }

    private Claims obtenerClaims(String token) {

        return Jwts.parser()
                .verifyWith(obtenerClave())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}