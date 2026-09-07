package cl.duoc.bank_batch.bff.web;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/bff/web")
public class WebBffController {

    private final JdbcTemplate jdbcTemplate;

    public WebBffController(JdbcTemplate jdbcTemplate) {
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

        Long cuentasIntereses = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cuentas_intereses",
                Long.class
        );

        Long resumenesAnuales = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM resumen_anual",
                Long.class
        );

        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put(
                "transaccionesProcesadas",
                transaccionesProcesadas
        );
        resumen.put(
                "resumenesDiarios",
                resumenesDiarios
        );
        resumen.put(
                "cuentasConIntereses",
                cuentasIntereses
        );
        resumen.put(
                "resumenesAnuales",
                resumenesAnuales
        );

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("canal", "web");
        respuesta.put("descripcion", "BFF Web Banco XYZ");
        respuesta.put("resumen", resumen);

        return respuesta;
    }
}