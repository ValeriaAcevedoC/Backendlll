package cl.duoc.bank_batch.processor;

import cl.duoc.bank_batch.model.MovimientoAnual;

import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class MovimientoAnualProcessor
        implements ItemProcessor<MovimientoAnual, MovimientoAnual> {

    private static final DateTimeFormatter[] FORMATOS_FECHA = {
            DateTimeFormatter.ISO_LOCAL_DATE,

            DateTimeFormatter.ofPattern("dd-MM-uuuu")
                    .withResolverStyle(ResolverStyle.STRICT),

            DateTimeFormatter.ofPattern("dd/MM/uuuu")
                    .withResolverStyle(ResolverStyle.STRICT),

            DateTimeFormatter.ofPattern("uuuu/MM/dd")
                    .withResolverStyle(ResolverStyle.STRICT)
    };

    @Override
    public MovimientoAnual process(MovimientoAnual movimiento) {

        movimiento.setAnomalia(false);
        movimiento.setDetalleAnomalia(null);

        System.out.println(
                "Procesando movimiento cuenta "
                        + movimiento.getCuentaId()
                        + " | "
                        + movimiento.getTransaccion()
                        + " | "
                        + movimiento.getMonto()
        );

        List<String> anomalias = new ArrayList<>();

        // -------------------------------------------------
        // VALIDACION Y NORMALIZACION DE FECHA
        // -------------------------------------------------

        if (movimiento.getFecha() == null
                || movimiento.getFecha().isBlank()) {

            throw new DateTimeParseException(
                    "Fecha nula o vacia",
                    "",
                    0
            );
        }

        String fechaOriginal = movimiento.getFecha().trim();

        LocalDate fechaNormalizada = convertirFecha(fechaOriginal);

        movimiento.setFecha(
                fechaNormalizada.format(DateTimeFormatter.ISO_LOCAL_DATE)
        );

        // -------------------------------------------------
        // VALIDACION DE MONTO
        // -------------------------------------------------

        if (movimiento.getMonto() == null) {

            throw new IllegalArgumentException(
                    "Monto nulo en cuenta "
                            + movimiento.getCuentaId()
            );

        } else if (movimiento.getMonto().signum() == 0) {

            anomalias.add("Monto igual a cero");
        }

        // -------------------------------------------------
        // NORMALIZACION Y VALIDACION DEL TIPO DE MOVIMIENTO
        // -------------------------------------------------

        if (movimiento.getTransaccion() == null
                || movimiento.getTransaccion().isBlank()) {

            anomalias.add("Tipo de movimiento nulo o vacio");

        } else {

            String tipoOriginal = movimiento.getTransaccion();

            String tipoNormalizado = normalizarTexto(tipoOriginal);

            if (!tipoNormalizado.equals("deposito")
                    && !tipoNormalizado.equals("retiro")
                    && !tipoNormalizado.equals("compra")) {

                anomalias.add(
                        "Tipo de movimiento invalido: " + tipoOriginal
                );

            } else {

                movimiento.setTransaccion(tipoNormalizado);
            }
        }

        // -------------------------------------------------
        // VALIDACION DE DESCRIPCION
        // -------------------------------------------------

        if (movimiento.getDescripcion() == null
                || movimiento.getDescripcion().isBlank()) {

            anomalias.add("Descripcion nula o vacia");

        } else {

            movimiento.setDescripcion(
                    movimiento.getDescripcion().trim()
            );
        }

        // -------------------------------------------------
        // RESULTADO DE LAS VALIDACIONES
        // -------------------------------------------------

        if (!anomalias.isEmpty()) {

            movimiento.setAnomalia(true);

            movimiento.setDetalleAnomalia(
                    String.join(" | ", anomalias)
            );

            System.out.println(
                    "ANOMALIA cuenta "
                            + movimiento.getCuentaId()
                            + ": "
                            + movimiento.getDetalleAnomalia()
            );
        }

        return movimiento;
    }

    // -------------------------------------------------
    // CONVERSION DE FECHAS
    // -------------------------------------------------

    private LocalDate convertirFecha(String fecha) {

        DateTimeParseException ultimoError = null;

        for (DateTimeFormatter formato : FORMATOS_FECHA) {

            try {

                return LocalDate.parse(fecha, formato);

            } catch (DateTimeParseException e) {

                ultimoError = e;
            }
        }

        throw new DateTimeParseException(
                "Fecha invalida: " + fecha,
                fecha,
                0,
                ultimoError
        );
    }

    // -------------------------------------------------
    // NORMALIZACION DE TEXTO
    // -------------------------------------------------

    private String normalizarTexto(String texto) {

        String normalizado = Normalizer.normalize(
                texto,
                Normalizer.Form.NFD
        );

        normalizado = normalizado.replaceAll("\\p{M}", "");

        return normalizado
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}