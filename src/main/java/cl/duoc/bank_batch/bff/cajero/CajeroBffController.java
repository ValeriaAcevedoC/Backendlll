package cl.duoc.bank_batch.bff.cajero;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
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
    public Map<String, Object> consultarSaldo(
            @PathVariable Long cuentaId) {

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

        Map<String, Object> respuesta = new LinkedHashMap<>();

        respuesta.put("canal", "cajero");
        respuesta.put(
                "cuentaId",
                cuenta.get("cuenta_id")
        );
        respuesta.put(
                "saldoDisponible",
                cuenta.get("saldo_final")
        );

        return respuesta;
    }

    // -------------------------------------------------
    // RETIRO
    // -------------------------------------------------

    @PostMapping("/retiro/{cuentaId}")
    public Map<String, Object> realizarRetiro(
            @PathVariable Long cuentaId,
            @RequestBody Map<String, BigDecimal> solicitud) {

        BigDecimal monto = solicitud.get("monto");

        return cajeroService.realizarRetiro(
                cuentaId,
                monto
        );
    }
}