package cl.duoc.bank_batch.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class ClientesClient {

    private final RestClient restClient;

    public ClientesClient(
            @Qualifier("loadBalancedRestClientBuilder") RestClient.Builder restClientBuilder,
            @Value("${services.clientes.base-url:http://clientes-service}") String baseUrl) {

        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .requestInterceptor((request, body, execution) -> {
                    Authentication authentication =
                            SecurityContextHolder.getContext().getAuthentication();

                    if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
                        request.getHeaders().setBearerAuth(
                                jwtAuthentication.getToken().getTokenValue()
                        );
                    }

                    return execution.execute(request, body);
                })
                .build();
    }

    @CircuitBreaker(name = "clientesService", fallbackMethod = "fallbackClientes")
    public String obtenerClientes() {
        return restClient.get()
                .uri("/api/clientes")
                .retrieve()
                .body(String.class);
    }

    public String fallbackClientes(Throwable t) {
        return "{\"error\":\"clientes-service no disponible temporalmente\"}";
    }
}
