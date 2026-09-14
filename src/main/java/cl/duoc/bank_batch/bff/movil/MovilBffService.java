package cl.duoc.bank_batch.bff.movil;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class MovilBffService {

    private final JdbcTemplate jdbcTemplate;

    public MovilBffService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public MovilResumenDTO obtenerResumen() {

        Long transaccionesProcesadas = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transacciones_procesadas",
                Long.class
        );

        Long resumenesDiarios = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM resumen_diario",
                Long.class
        );

        return new MovilResumenDTO(
                "movil",
                transaccionesProcesadas,
                resumenesDiarios
        );
    }
}