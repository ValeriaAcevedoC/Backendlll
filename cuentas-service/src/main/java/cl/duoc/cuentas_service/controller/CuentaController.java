package cl.duoc.cuentas_service.controller;

import cl.duoc.cuentas_service.model.Cuenta;
import cl.duoc.cuentas_service.repository.CuentaRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cuentas")
public class CuentaController {

    private final CuentaRepository repository;

    public CuentaController(CuentaRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Cuenta> listar() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public Cuenta buscar(@PathVariable Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cuenta no encontrada"));
    }
}