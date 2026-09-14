package cl.duoc.bank_batch.security;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtService jwtService) {

        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestBody Map<String, String> solicitud) {

        String usuario = solicitud.get("usuario");
        String password = solicitud.get("password");

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                usuario,
                                password
                        )
                );

        List<String> roles = authentication
        .getAuthorities()
        .stream()
        .map(GrantedAuthority::getAuthority)
        .filter(rol -> rol.startsWith("ROLE_"))
        .toList();

        String token = jwtService.generarToken(
                authentication.getName(),
                roles
        );

        Map<String, Object> respuesta =
                new LinkedHashMap<>();

        respuesta.put("usuario", authentication.getName());
        respuesta.put("roles", roles);
        respuesta.put("token", token);
        respuesta.put("tipo", "Bearer");

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(respuesta);
    }
}