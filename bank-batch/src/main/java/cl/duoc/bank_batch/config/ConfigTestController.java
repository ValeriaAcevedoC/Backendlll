package cl.duoc.bank_batch.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import cl.duoc.bank_batch.client.ClientesClient;

@RestController
public class ConfigTestController {

    @Value("${mensaje.origen}")
    private String mensajeOrigen;

    @GetMapping("/api/debug/config")
    public String verConfig() {
        return mensajeOrigen;
    }

    @Autowired
    private ClientesClient clientesClient;

    @GetMapping("/api/debug/clientes")
    public String obtenerClientes() {
        return clientesClient.obtenerClientes();
    }
}