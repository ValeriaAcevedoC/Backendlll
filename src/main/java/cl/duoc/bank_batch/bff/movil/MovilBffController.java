package cl.duoc.bank_batch.bff.movil;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bff/movil")
public class MovilBffController {

    private final MovilBffService movilBffService;

    public MovilBffController(MovilBffService movilBffService) {
        this.movilBffService = movilBffService;
    }

    @GetMapping("/resumen")
    public MovilResumenDTO obtenerResumen() {
        return movilBffService.obtenerResumen();
    }
}