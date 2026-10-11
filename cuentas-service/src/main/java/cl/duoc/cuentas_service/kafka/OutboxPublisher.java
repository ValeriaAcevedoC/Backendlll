package cl.duoc.cuentas_service.kafka;

import java.util.concurrent.TimeUnit;
import org.slf4j.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    name = "outbox.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class OutboxPublisher {
  private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
  private final JdbcTemplate jdbc;
  private final KafkaTemplate<String, String> kafka;

  public OutboxPublisher(JdbcTemplate j, KafkaTemplate<String, String> k) {
    jdbc = j;
    kafka = k;
  }

  @Scheduled(fixedDelayString = "${outbox.interval-ms:5000}")
  @Transactional
  public void publicar() {
    var rows =
        jdbc.queryForList(
            "SELECT id,contenido FROM eventos_pendientes WHERE publicado=FALSE ORDER BY fecha LIMIT"
                + " 20 FOR UPDATE SKIP LOCKED");
    for (var row : rows) {
      String id = (String) row.get("id");
      try {
        kafka.send("banco.operaciones", id, (String) row.get("contenido")).get(5, TimeUnit.SECONDS);
        jdbc.update(
            "UPDATE eventos_pendientes SET publicado=TRUE,intentos=intentos+1 WHERE id=?", id);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      } catch (Exception e) {
        jdbc.update("UPDATE eventos_pendientes SET intentos=intentos+1 WHERE id=?", id);
        log.warn("Evento {} pendiente; Kafka no confirmó la entrega", id);
      }
    }
  }
}
