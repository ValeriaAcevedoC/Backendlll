package cl.duoc.bank_batch.processor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import cl.duoc.bank_batch.model.CuentaInteres;

import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class InteresProcessor
        implements ItemProcessor<CuentaInteres, CuentaInteres> {

    @Override
    public CuentaInteres process(CuentaInteres cuenta) {

        cuenta.setValida(true);
        cuenta.setObservacion(null);

        List<String> observaciones = new ArrayList<>();

        // -------------------------------------------------
        // VALIDACION DE NOMBRE
        // -------------------------------------------------

        if (cuenta.getNombre() == null
                || cuenta.getNombre().isBlank()) {

            observaciones.add("Nombre nulo o vacio");

        } else {

            cuenta.setNombre(
                    cuenta.getNombre().trim()
            );
        }

        // -------------------------------------------------
        // VALIDACION DE EDAD
        // -------------------------------------------------

        if (cuenta.getEdad() == null) {

            observaciones.add("Edad nula");

        } else if (cuenta.getEdad() <= 0
                || cuenta.getEdad() > 120) {

            observaciones.add(
                    "Edad fuera de rango: "
                            + cuenta.getEdad()
            );
        }

        // -------------------------------------------------
        // VALIDACION DEL SALDO
        // -------------------------------------------------

        if (cuenta.getSaldo() == null) {

            observaciones.add("Saldo nulo");

            cuenta.setValida(false);

            cuenta.setTasaInteres(
                    BigDecimal.ZERO
            );

            cuenta.setInteresCalculado(
                    BigDecimal.ZERO
            );

            cuenta.setSaldoFinal(
                    BigDecimal.ZERO
            );

            cuenta.setObservacion(
                    String.join(
                            " | ",
                            observaciones
                    )
            );

            System.out.println(
                    "Cuenta "
                            + cuenta.getCuentaId()
                            + " no procesada: "
                            + cuenta.getObservacion()
            );

            return cuenta;
        }

        // -------------------------------------------------
        // NORMALIZACION DEL TIPO DE CUENTA
        // -------------------------------------------------

        String tipoNormalizado = null;

        if (cuenta.getTipo() == null
                || cuenta.getTipo().isBlank()) {

            observaciones.add(
                    "Tipo de cuenta nulo o vacio"
            );

        } else {

            tipoNormalizado =
                    normalizarTexto(cuenta.getTipo());

            if (!tipoNormalizado.equals("ahorro")
                    && !tipoNormalizado.equals("prestamo")) {

                observaciones.add(
                        "Tipo de cuenta no procesable: "
                                + cuenta.getTipo()
                );

            } else {

                cuenta.setTipo(tipoNormalizado);
            }
        }

        // -------------------------------------------------
        // SI EXISTEN ERRORES QUE IMPIDEN CALCULAR
        // -------------------------------------------------

        if (tipoNormalizado == null
                || (!tipoNormalizado.equals("ahorro")
                && !tipoNormalizado.equals("prestamo"))) {

            cuenta.setValida(false);

            cuenta.setTasaInteres(
                    BigDecimal.ZERO
            );

            cuenta.setInteresCalculado(
                    BigDecimal.ZERO
            );

            cuenta.setSaldoFinal(
                    cuenta.getSaldo()
            );

            cuenta.setObservacion(
                    String.join(
                            " | ",
                            observaciones
                    )
            );

            System.out.println(
                    "Cuenta "
                            + cuenta.getCuentaId()
                            + " no procesada: "
                            + cuenta.getObservacion()
            );

            return cuenta;
        }

        // -------------------------------------------------
        // CALCULO DE LA TASA
        // -------------------------------------------------

        BigDecimal tasa;

        if (tipoNormalizado.equals("ahorro")) {

            tasa = new BigDecimal("0.01");

        } else {

            tasa = new BigDecimal("0.02");
        }

        // -------------------------------------------------
        // CALCULO DE INTERES Y SALDO FINAL
        // -------------------------------------------------

        BigDecimal interes = cuenta.getSaldo()
                .multiply(tasa)
                .setScale(
                        2,
                        RoundingMode.HALF_UP
                );

        BigDecimal saldoFinal =
                cuenta.getSaldo().add(interes);

        cuenta.setTasaInteres(tasa);
        cuenta.setInteresCalculado(interes);
        cuenta.setSaldoFinal(saldoFinal);

        // -------------------------------------------------
        // REGISTRO DE OTRAS OBSERVACIONES
        // -------------------------------------------------

        if (!observaciones.isEmpty()) {

            cuenta.setValida(false);

            cuenta.setObservacion(
                    String.join(
                            " | ",
                            observaciones
                    )
            );

            System.out.println(
                    "Cuenta "
                            + cuenta.getCuentaId()
                            + " procesada con observaciones: "
                            + cuenta.getObservacion()
            );

        } else {

            cuenta.setValida(true);
            cuenta.setObservacion(null);
        }

        System.out.println(
                "Cuenta "
                        + cuenta.getCuentaId()
                        + " | tipo: "
                        + cuenta.getTipo()
                        + " | interes: "
                        + interes
                        + " | saldo final: "
                        + saldoFinal
        );

        return cuenta;
    }

    // -------------------------------------------------
    // NORMALIZACION DE TEXTO
    // -------------------------------------------------

    private String normalizarTexto(String texto) {

        String normalizado = Normalizer.normalize(
                texto,
                Normalizer.Form.NFD
        );

        normalizado =
                normalizado.replaceAll("\\p{M}", "");

        return normalizado
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}