package cl.duoc.bank_batch.bff.cajero;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class CajeroService {

    private final JdbcTemplate jdbcTemplate;

    public CajeroService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public Map<String, Object> realizarRetiro(
            Long cuentaId,
            BigDecimal monto) {

        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "El monto debe ser mayor a cero"
            );
        }

        BigDecimal saldoActual = jdbcTemplate.queryForObject(
                """
                SELECT saldo_final
                FROM cuentas_intereses
                WHERE cuenta_id = ?
                """,
                BigDecimal.class,
                cuentaId
        );

        if (saldoActual == null) {
            throw new IllegalArgumentException(
                    "La cuenta no existe"
            );
        }

        if (monto.compareTo(saldoActual) > 0) {
            throw new IllegalArgumentException(
                    "Saldo insuficiente"
            );
        }

        BigDecimal saldoPosterior =
                saldoActual.subtract(monto);

        jdbcTemplate.update(
                """
                UPDATE cuentas_intereses
                SET saldo_final = ?
                WHERE cuenta_id = ?
                """,
                saldoPosterior,
                cuentaId
        );

        jdbcTemplate.update(
                """
                INSERT INTO retiros_cajero (
                    cuenta_id,
                    monto,
                    saldo_anterior,
                    saldo_posterior
                )
                VALUES (?, ?, ?, ?)
                """,
                cuentaId,
                monto,
                saldoActual,
                saldoPosterior
        );

        Map<String, Object> respuesta =
                new LinkedHashMap<>();

        respuesta.put("canal", "cajero");
        respuesta.put("cuentaId", cuentaId);
        respuesta.put("montoRetirado", monto);
        respuesta.put("saldoAnterior", saldoActual);
        respuesta.put("saldoDisponible", saldoPosterior);

        return respuesta;
    }
}