package cl.duoc.bank_batch.bff.cajero;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/bff/cajero")
public class CajeroBffController {

    private final JdbcTemplate jdbcTemplate;
    private final CajeroService cajeroService;

    public CajeroBffController(
            JdbcTemplate jdbcTemplate,
            CajeroService cajeroService) {

        this.jdbcTemplate = jdbcTemplate;
        this.cajeroService = cajeroService;
    }

    // -------------------------------------------------
    // CONSULTA DE SALDO
    // -------------------------------------------------

    @GetMapping("/saldo/{cuentaId}")
    public SaldoCajeroDTO consultarSaldo(
            @PathVariable Long cuentaId) {

        try {
            Map<String, Object> cuenta = jdbcTemplate.queryForMap(
                    """
                    SELECT
                        cuenta_id,
                        saldo_final
                    FROM cuentas_intereses
                    WHERE cuenta_id = ?
                    """,
                    cuentaId
            );

            return new SaldoCajeroDTO(
                    "cajero",
                    ((Number) cuenta.get("cuenta_id")).longValue(),
                    (BigDecimal) cuenta.get("saldo_final")
            );

        } catch (EmptyResultDataAccessException e) {
            throw new CuentaNoEncontradaException(
                    "La cuenta " + cuentaId + " no existe"
            );
        }
    }

    // -------------------------------------------------
    // RETIRO
    // -------------------------------------------------

    @PostMapping("/retiro/{cuentaId}")
    public RetiroResponseDTO realizarRetiro(
            @PathVariable Long cuentaId,
            @RequestBody Map<String, BigDecimal> solicitud) {

        BigDecimal monto = solicitud.get("monto");

        return cajeroService.realizarRetiro(
                cuentaId,
                monto
        );
    }

    // -------------------------------------------------
    // EXCEPCION CUENTA NO ENCONTRADA
    // -------------------------------------------------

    @ResponseStatus(HttpStatus.NOT_FOUND)
    private static class CuentaNoEncontradaException
            extends RuntimeException {

        public CuentaNoEncontradaException(String mensaje) {
            super(mensaje);
        }
    }
}