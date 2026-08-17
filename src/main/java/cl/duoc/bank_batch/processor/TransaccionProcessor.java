package cl.duoc.bank_batch.processor;

import cl.duoc.bank_batch.model.Transaccion;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class TransaccionProcessor implements ItemProcessor<Transaccion, Transaccion> {

    @Override
    public Transaccion process(Transaccion transaccion) {

        System.out.println("Procesando transaccion: " + transaccion.getId());

        transaccion.setAnomalia(false);
        transaccion.setDetalleAnomalia(null);

        if (transaccion.getMonto() == null) {

            transaccion.setAnomalia(true);
            transaccion.setDetalleAnomalia("Monto nulo");

        } else if (transaccion.getMonto().signum() < 0) {

            transaccion.setAnomalia(true);
            transaccion.setDetalleAnomalia("Monto negativo");

        } else if (transaccion.getMonto().signum() == 0) {

            transaccion.setAnomalia(true);
            transaccion.setDetalleAnomalia("Monto igual a cero");
        }

        if (transaccion.getTipo() == null
                || (!transaccion.getTipo().equalsIgnoreCase("debito")
                && !transaccion.getTipo().equalsIgnoreCase("credito"))) {

            transaccion.setAnomalia(true);
            transaccion.setDetalleAnomalia("Tipo de transaccion invalido");
        }

        if (transaccion.isAnomalia()) {
            System.out.println(
                    "ANOMALIA ID " + transaccion.getId()
                    + ": " + transaccion.getDetalleAnomalia()
            );
        }

        return transaccion;
    }
}