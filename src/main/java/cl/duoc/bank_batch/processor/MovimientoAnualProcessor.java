package cl.duoc.bank_batch.processor;

import cl.duoc.bank_batch.model.MovimientoAnual;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class MovimientoAnualProcessor
        implements ItemProcessor<MovimientoAnual, MovimientoAnual> {

    @Override
    public MovimientoAnual process(MovimientoAnual movimiento) {

        movimiento.setAnomalia(false);
        movimiento.setDetalleAnomalia(null);

        System.out.println(
                "Procesando movimiento cuenta "
                + movimiento.getCuentaId()
                + " | " + movimiento.getTransaccion()
                + " | " + movimiento.getMonto()
        );

        if (movimiento.getMonto() == null) {
            movimiento.setAnomalia(true);
            movimiento.setDetalleAnomalia("Monto nulo");

        } else if (movimiento.getMonto().signum() == 0) {
            movimiento.setAnomalia(true);
            movimiento.setDetalleAnomalia("Monto igual a cero");
        }

        if (movimiento.getTransaccion() == null
                || (!movimiento.getTransaccion().equalsIgnoreCase("deposito")
                && !movimiento.getTransaccion().equalsIgnoreCase("retiro")
                && !movimiento.getTransaccion().equalsIgnoreCase("compra"))) {

            movimiento.setAnomalia(true);
            movimiento.setDetalleAnomalia("Tipo de movimiento invalido");
        }

        if (movimiento.isAnomalia()) {
            System.out.println(
                    "ANOMALIA cuenta "
                    + movimiento.getCuentaId()
                    + ": "
                    + movimiento.getDetalleAnomalia()
            );
        }

        return movimiento;
    }
}