package cl.duoc.bank_batch.policy;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.stereotype.Component;

@Component
public class DataQualityDecider implements JobExecutionDecider {

    public static final String CALIDAD_INSUFICIENTE = "CALIDAD_INSUFICIENTE";
    private static final double PORCENTAJE_MAXIMO_OMISIONES = 0.10;

    @Override
    public FlowExecutionStatus decide(JobExecution jobExecution, StepExecution stepExecution) {
        if (stepExecution == null) {
            return FlowExecutionStatus.COMPLETED;
        }

        long registrosLeidos = stepExecution.getReadCount();
        long registrosConErrorLectura = stepExecution.getReadSkipCount();
        long registrosOmitidos = stepExecution.getSkipCount();
        long totalEvaluado = registrosLeidos + registrosConErrorLectura;

        if (totalEvaluado == 0) {
            return FlowExecutionStatus.COMPLETED;
        }

        double porcentajeOmisiones = (double) registrosOmitidos / totalEvaluado;

        if (porcentajeOmisiones > PORCENTAJE_MAXIMO_OMISIONES) {
            System.out.println(
                    "Calidad de datos insuficiente. Omisiones: "
                    + registrosOmitidos + "/" + totalEvaluado
                    + " (" + String.format("%.2f", porcentajeOmisiones * 100) + "%)"
            );
            return new FlowExecutionStatus(CALIDAD_INSUFICIENTE);
        }

        return FlowExecutionStatus.COMPLETED;
    }
}
