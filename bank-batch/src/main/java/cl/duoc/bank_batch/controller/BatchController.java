package cl.duoc.bank_batch.controller;

import cl.duoc.bank_batch.service.BatchOperations;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/batch")
public class BatchController {
  private final BatchOperations batch;

  public BatchController(BatchOperations b) {
    batch = b;
  }

  @PostMapping("/jobs/{nombre}")
  public Map<String, Object> iniciar(@PathVariable String nombre, @RequestBody Solicitud s)
      throws Exception {
    if (s.periodo() == null || !s.periodo().matches("\\d{4}-(0[1-9]|1[0-2])"))
      throw new IllegalArgumentException("Período esperado: yyyy-MM");
    return batch.iniciar(nombre, s.periodo(), s.ejecucion());
  }

  @GetMapping("/ejecuciones/{id}")
  public Map<String, Object> consultar(@PathVariable Long id) {
    return batch.consultar(id);
  }

  public record Solicitud(String periodo, String ejecucion) {}
}
