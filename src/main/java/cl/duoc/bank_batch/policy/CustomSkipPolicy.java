package cl.duoc.bank_batch.policy;

import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;
import org.springframework.stereotype.Component;

@Component
public class CustomSkipPolicy implements SkipPolicy {

    private static final int LIMITE_SKIPS = 3;

    @Override
    public boolean shouldSkip(Throwable t, long skipCount) throws SkipLimitExceededException {

        // Excepciones de datos "mal formados" que se pueden saltar:
        // - una linea del CSV que no calza con el formato esperado
        // - un campo numerico/fecha que no se puede parsear
        boolean esExcepcionSaltable =
                t instanceof FlatFileParseException
                || t instanceof NumberFormatException;

        if (!esExcepcionSaltable) {
            return false;
        }

        if (skipCount >= LIMITE_SKIPS) {
            System.out.println(
                    "Limite de skips superado (" + LIMITE_SKIPS + "). El Step debe fallar: "
                    + t.getClass().getSimpleName() + " - " + t.getMessage()
            );
            throw new SkipLimitExceededException(LIMITE_SKIPS, t);
        }

        System.out.println(
                "Registro omitido (skip #" + (skipCount + 1) + "): "
                + t.getClass().getSimpleName() + " - " + t.getMessage()
        );

        return true;
        
    }
}
