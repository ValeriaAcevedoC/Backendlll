package cl.duoc.bank_batch.controller;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/batch/reportes")
public class ReporteController {
  private final JdbcTemplate jdbc;

  public ReporteController(JdbcTemplate j) {
    jdbc = j;
  }

  @GetMapping("/diario")
  public List<Map<String, Object>> diario() {
    return jdbc.queryForList("SELECT * FROM resumen_diario ORDER BY fecha");
  }

  @GetMapping("/intereses")
  public List<Map<String, Object>> intereses(@RequestParam String periodo) {
    java.time.YearMonth.parse(periodo);
    return jdbc.queryForList(
        "SELECT * FROM resultados_intereses_mensuales WHERE periodo=? ORDER BY cuenta_id", periodo);
  }

  @GetMapping("/anual")
  public List<Map<String, Object>> anual(@RequestParam int anio) {
    return jdbc.queryForList(
        "SELECT * FROM estados_cuenta_anuales WHERE anio=? ORDER BY cuenta_id", anio);
  }
}
