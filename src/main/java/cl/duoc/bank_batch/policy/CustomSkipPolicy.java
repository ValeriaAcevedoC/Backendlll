package cl.duoc.bank_batch.policy;

import java.time.format.DateTimeParseException;

import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;
import org.springframework.stereotype.Component;

@Component
public class CustomSkipPolicy implements SkipPolicy {

    /*
     * Se usa un limite de 100 omisiones porque la data oficial
     * contiene aproximadamente 1000 registros.
     *
     * Esto permite tolerar hasta cerca de un 10% de errores,
     * manteniendo coherencia con DataQualityDecider.
     */
    private static final int LIMITE_SKIPS = 100;

    @Override
    public boolean shouldSkip(
            Throwable t,
            long skipCount
    ) throws SkipLimitExceededException {

        boolean esExcepcionSaltable =
                t instanceof FlatFileParseException
                || t instanceof NumberFormatException
                || t instanceof DateTimeParseException
                || t instanceof IllegalArgumentException;

        if (!esExcepcionSaltable) {
            return false;
        }

        if (skipCount >= LIMITE_SKIPS) {

            System.out.println(
                    "Limite de skips superado ("
                            + LIMITE_SKIPS
                            + "). El Step debe fallar: "
                            + t.getClass().getSimpleName()
                            + " - "
                            + t.getMessage()
            );

            throw new SkipLimitExceededException(
                    LIMITE_SKIPS,
                    t
            );
        }

        System.out.println(
                "Registro omitido (skip #"
                        + (skipCount + 1)
                        + "): "
                        + t.getClass().getSimpleName()
                        + " - "
                        + t.getMessage()
        );

        return true;
    }
}