package cl.duoc.bank_batch.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConfigTestController {

    @Value("${mensaje.origen}")
    private String mensajeOrigen;

    @GetMapping("/api/debug/config")
    public String verConfig() {
        return mensajeOrigen;
    }
}