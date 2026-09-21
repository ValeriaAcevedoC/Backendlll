package cl.duoc.bank_batch.bff.web;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class WebBffService {

    private final JdbcTemplate jdbcTemplate;

    public WebBffService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public WebResumenDTO obtenerResumen() {

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

        WebResumenDTO.Resumen resumen =
                new WebResumenDTO.Resumen(
                        transaccionesProcesadas,
                        resumenesDiarios,
                        cuentasIntereses,
                        resumenesAnuales
                );

        return new WebResumenDTO(
                "web",
                "BFF Web Banco XYZ",
                resumen
        );
    }
}