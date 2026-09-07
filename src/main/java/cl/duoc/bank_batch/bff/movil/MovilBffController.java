package cl.duoc.bank_batch.bff.movil;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/bff/movil")
public class MovilBffController {

    private final JdbcTemplate jdbcTemplate;

    public MovilBffController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/resumen")
    public Map<String, Object> obtenerResumen() {

        Long transaccionesProcesadas = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transacciones_procesadas",
                Long.class
        );

        Long resumenesDiarios = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM resumen_diario",
                Long.class
        );

        Map<String, Object> respuesta = new LinkedHashMap<>();

        respuesta.put("canal", "movil");
        respuesta.put(
                "transacciones",
                transaccionesProcesadas
        );
        respuesta.put(
                "resumenesDiarios",
                resumenesDiarios
        );

        return respuesta;
    }
}