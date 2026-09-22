package cl.duoc.bank_batch.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class ClientesClient {

    private final RestClient restClient = RestClient.create();

    @CircuitBreaker(name = "clientesService", fallbackMethod = "fallbackClientes")
    public String obtenerClientes() {
        return restClient.get()
                .uri("http://localhost:8091/api/clientes")
                .header("Authorization", "Basic YWRtaW46YWRtaW4xMjM=")
                .retrieve()
                .body(String.class);
    }

    public String fallbackClientes(Throwable t) {
        return "{\"error\":\"clientes-service no disponible temporalmente\"}";
    }
}