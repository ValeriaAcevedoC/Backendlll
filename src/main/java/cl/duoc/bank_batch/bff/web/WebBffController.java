package cl.duoc.bank_batch.bff.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bff/web")
public class WebBffController {

    private final WebBffService webBffService;

    public WebBffController(WebBffService webBffService) {
        this.webBffService = webBffService;
    }

    @GetMapping("/resumen")
    public WebResumenDTO obtenerResumen() {
        return webBffService.obtenerResumen();
    }
}