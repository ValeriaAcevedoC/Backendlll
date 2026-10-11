package cl.duoc.cuentas_service.kafka;

import cl.duoc.bank_common.OperacionDTO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class AuditoriaConsumer {
  private final JdbcTemplate jdbc;
  private final ObjectMapper mapper;

  public AuditoriaConsumer(JdbcTemplate j, ObjectMapper m) {
    jdbc = j;
    mapper = m;
  }

  @KafkaListener(topics = "banco.operaciones", groupId = "cuentas-auditoria")
  public void recibir(String contenido) {
    var evento = mapper.readValue(contenido, OperacionDTO.class);
    if (evento.idempotencia() == null)
      throw new IllegalArgumentException("Evento sin identificador");
    try {
      jdbc.update(
          "INSERT INTO historial_eventos(id,contenido) VALUES(?,?)",
          evento.idempotencia(),
          contenido);
    } catch (DuplicateKeyException ignored) {
      /* Entrega al menos una vez: no duplicar auditoría. */
    }
  }
}
