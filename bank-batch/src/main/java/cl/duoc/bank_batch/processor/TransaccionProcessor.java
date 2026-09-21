package cl.duoc.bank_batch.processor;

import cl.duoc.bank_batch.model.Transaccion;
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
public class TransaccionProcessor implements ItemProcessor<Transaccion, Transaccion> {

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
    public Transaccion process(Transaccion transaccion) {

        System.out.println("Procesando transaccion: " + transaccion.getId());

        transaccion.setAnomalia(false);
        transaccion.setDetalleAnomalia(null);

        List<String> anomalias = new ArrayList<>();

        // -------------------------------------------------
        // VALIDACION Y NORMALIZACION DE FECHA
        // -------------------------------------------------

        if (transaccion.getFecha() == null
                || transaccion.getFecha().isBlank()) {

            anomalias.add("Fecha nula o vacia");
            transaccion.setFecha(null);

        } else {

            String fechaOriginal = transaccion.getFecha().trim();

            try {

                LocalDate fechaNormalizada =
                        convertirFecha(fechaOriginal);

                transaccion.setFecha(
                        fechaNormalizada.format(
                                DateTimeFormatter.ISO_LOCAL_DATE
                        )
                );

            } catch (DateTimeParseException e) {

                anomalias.add(
                        "Fecha invalida: " + fechaOriginal
                );

                transaccion.setFecha(null);
            }
        }

        // -------------------------------------------------
        // VALIDACION DE MONTO
        // -------------------------------------------------

        if (transaccion.getMonto() == null) {

            anomalias.add("Monto nulo");

        } else if (transaccion.getMonto().signum() < 0) {

            anomalias.add("Monto negativo");

        } else if (transaccion.getMonto().signum() == 0) {

            anomalias.add("Monto igual a cero");
        }

        // -------------------------------------------------
        // NORMALIZACION Y VALIDACION DE TIPO
        // -------------------------------------------------

        if (transaccion.getTipo() == null
                || transaccion.getTipo().isBlank()) {

            anomalias.add(
                    "Tipo de transaccion nulo o vacio"
            );

        } else {

            String tipoOriginal = transaccion.getTipo();

            String tipoNormalizado =
                    normalizarTexto(tipoOriginal);

            if (!tipoNormalizado.equals("debito")
                    && !tipoNormalizado.equals("credito")) {

                anomalias.add(
                        "Tipo de transaccion invalido: "
                                + tipoOriginal
                );

            } else {

                transaccion.setTipo(tipoNormalizado);
            }
        }

        // -------------------------------------------------
        // RESULTADO DE VALIDACIONES
        // -------------------------------------------------

        if (!anomalias.isEmpty()) {

            transaccion.setAnomalia(true);

            transaccion.setDetalleAnomalia(
                    String.join(" | ", anomalias)
            );

            System.out.println(
                    "ANOMALIA ID "
                            + transaccion.getId()
                            + ": "
                            + transaccion.getDetalleAnomalia()
            );
        }

        return transaccion;
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

        throw ultimoError;
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