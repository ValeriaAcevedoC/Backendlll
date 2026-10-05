package cl.duoc.bank_batch.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class TransaccionConsumer {

    private final ObjectMapper objectMapper;

    public TransaccionConsumer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "banco.transacciones",
            groupId = "bank-batch-group"
    )
    public void recibirMensaje(String mensaje) {

        try {

            TransaccionEvento evento =
                    objectMapper.readValue(
                            mensaje,
                            TransaccionEvento.class
                    );

            System.out.println(
                    "===== EVENTO BANCARIO RECIBIDO ====="
            );

            System.out.println(
                    "Tipo: " + evento.tipo()
            );

            System.out.println(
                    "Cuenta: " + evento.cuentaId()
            );

            System.out.println(
                    "Monto: " + evento.monto()
            );

            System.out.println(
                    "Saldo anterior: " + evento.saldoAnterior()
            );

            System.out.println(
                    "Saldo posterior: " + evento.saldoPosterior()
            );

            System.out.println(
                    "Fecha: " + evento.fecha()
            );

            System.out.println(
                    "=================================="
            );

        } catch (JsonProcessingException e) {

            // Mantiene compatibilidad con los mensajes de prueba antiguos
            System.out.println(
                    "Evento Kafka recibido como texto: " + mensaje
            );
        }
    }
}