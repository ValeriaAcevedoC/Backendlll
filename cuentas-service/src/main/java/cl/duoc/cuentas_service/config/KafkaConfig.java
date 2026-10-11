package cl.duoc.cuentas_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.*;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.listener.*;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {
  @Bean
  public NewTopic operacionesTopic() {
    return TopicBuilder.name("banco.operaciones").partitions(3).replicas(1).build();
  }

  @Bean
  public NewTopic erroresTopic() {
    return TopicBuilder.name("banco.operaciones.DLT").partitions(3).replicas(1).build();
  }

  @Bean
  public DefaultErrorHandler kafkaErrorHandler(
      org.springframework.kafka.core.KafkaTemplate<String, String> template) {
    var recoverer = new DeadLetterPublishingRecoverer(template);
    recoverer.setFailIfSendResultIsError(true);
    return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L));
  }
}
