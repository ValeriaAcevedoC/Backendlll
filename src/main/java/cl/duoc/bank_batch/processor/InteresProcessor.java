package cl.duoc.bank_batch.processor;

import java.math.BigDecimal;
import java.math.RoundingMode;

import cl.duoc.bank_batch.model.CuentaInteres;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class InteresProcessor implements ItemProcessor<CuentaInteres, CuentaInteres> {

    @Override
    public CuentaInteres process(CuentaInteres cuenta) {

        cuenta.setValida(true);
        cuenta.setObservacion(null);

        BigDecimal tasa;

        if ("ahorro".equalsIgnoreCase(cuenta.getTipo())) {
            tasa = new BigDecimal("0.01");

        } else if ("prestamo".equalsIgnoreCase(cuenta.getTipo())) {
            tasa = new BigDecimal("0.02");

        } else {
            cuenta.setValida(false);
            cuenta.setObservacion("Tipo de cuenta no procesable");
            cuenta.setTasaInteres(BigDecimal.ZERO);
            cuenta.setInteresCalculado(BigDecimal.ZERO);
            cuenta.setSaldoFinal(cuenta.getSaldo());

            System.out.println(
                    "Cuenta " + cuenta.getCuentaId()
                    + " no procesada: " + cuenta.getTipo()
            );

            return cuenta;
        }

        BigDecimal interes = cuenta.getSaldo()
                .multiply(tasa)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal saldoFinal;

        if ("ahorro".equalsIgnoreCase(cuenta.getTipo())) {
            saldoFinal = cuenta.getSaldo().add(interes);
        } else {
            saldoFinal = cuenta.getSaldo().add(interes);
        }

        cuenta.setTasaInteres(tasa);
        cuenta.setInteresCalculado(interes);
        cuenta.setSaldoFinal(saldoFinal);

        System.out.println(
                "Cuenta " + cuenta.getCuentaId()
                + " | tipo: " + cuenta.getTipo()
                + " | interes: " + interes
                + " | saldo final: " + saldoFinal
        );

        return cuenta;
    }
}