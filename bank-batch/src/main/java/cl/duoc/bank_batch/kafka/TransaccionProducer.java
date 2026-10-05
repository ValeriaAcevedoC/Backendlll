package cl.duoc.bank_batch.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class TransaccionProducer {

    private static final String TOPIC = "banco.transacciones";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public TransaccionProducer(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper) {

        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    // Se mantiene para las pruebas anteriores de Kafka
    public void enviarMensaje(String mensaje) {

        kafkaTemplate.send(TOPIC, mensaje);

        System.out.println(
                "Evento enviado a Kafka [" + TOPIC + "]: " + mensaje
        );
    }

    // Nuevo método para enviar una transacción bancaria real
    public void enviarTransaccion(TransaccionEvento evento) {

        try {

            String eventoJson =
                    objectMapper.writeValueAsString(evento);

            kafkaTemplate.send(
                    TOPIC,
                    evento.cuentaId().toString(),
                    eventoJson
            );

            System.out.println(
                    "Transacción enviada a Kafka [" +
                    TOPIC +
                    "]: " +
                    eventoJson
            );

        } catch (JsonProcessingException e) {

            throw new IllegalStateException(
                    "Error al convertir la transacción a JSON",
                    e
            );
        }
    }
}