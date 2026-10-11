package cl.duoc.cuentas_service.repository;

import cl.duoc.bank_common.CuentaDTO;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CuentaRepository {
  private final JdbcTemplate jdbc;

  public CuentaRepository(JdbcTemplate j) {
    jdbc = j;
  }

  public List<CuentaDTO> findAll() {
    return jdbc.query("SELECT * FROM cuentas ORDER BY id", this::map);
  }

  public Optional<CuentaDTO> findById(Long id) {
    return jdbc.query("SELECT * FROM cuentas WHERE id=?", this::map, id).stream().findFirst();
  }

  public CuentaDTO map(java.sql.ResultSet r, int n) throws java.sql.SQLException {
    return new CuentaDTO(
        r.getLong("id"),
        r.getLong("cliente_id"),
        r.getString("tipo"),
        r.getBigDecimal("saldo"),
        r.getString("estado"),
        r.getTimestamp("fecha_apertura").toLocalDateTime());
  }
}
