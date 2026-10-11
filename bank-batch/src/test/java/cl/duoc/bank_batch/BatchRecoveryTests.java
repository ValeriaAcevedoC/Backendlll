package cl.duoc.bank_batch;

import static org.mockito.Mockito.*;

import cl.duoc.bank_batch.service.BatchOperations;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.*;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;

class BatchRecoveryTests {
  @Test
  void reiniciaFalloTemporalConLimite() throws Exception {
    Job job = mock(Job.class);
    when(job.getName()).thenReturn("interesJob");
    JobOperator operator = mock(JobOperator.class);
    JobRepository repository = mock(JobRepository.class);
    var instance = new JobInstance(1, "interesJob");
    var execution = new JobExecution(1, instance, new JobParametersBuilder().toJobParameters());
    execution.setStatus(BatchStatus.FAILED);
    execution.addFailureException(
        new org.springframework.dao.TransientDataAccessResourceException("Base desconectada"));
    when(repository.getJobInstances("interesJob", 0, 100)).thenReturn(List.of(instance));
    when(repository.getJobExecutions(instance)).thenReturn(List.of(execution));
    when(operator.restart(execution)).thenReturn(execution);
    new BatchOperations(List.of(job), operator, repository, 3).recuperar();
    verify(operator).restart(execution);
    clearInvocations(operator);
    when(repository.getJobExecutions(instance))
        .thenReturn(List.of(execution, execution, execution));
    new BatchOperations(List.of(job), operator, repository, 3).recuperar();
    verifyNoInteractions(operator);
  }

  @Test
  void calidadInsuficienteNoSeReintenta() {
    Job job = mock(Job.class);
    when(job.getName()).thenReturn("interesJob");
    JobOperator operator = mock(JobOperator.class);
    JobRepository repository = mock(JobRepository.class);
    var instance = new JobInstance(1, "interesJob");
    var execution = new JobExecution(1, instance, new JobParametersBuilder().toJobParameters());
    execution.setStatus(BatchStatus.FAILED);
    execution.setExitStatus(new org.springframework.batch.core.ExitStatus("CALIDAD_INSUFICIENTE"));
    when(repository.getJobInstances("interesJob", 0, 100)).thenReturn(List.of(instance));
    when(repository.getJobExecutions(instance)).thenReturn(List.of(execution));
    new BatchOperations(List.of(job), operator, repository, 3).recuperar();
    verifyNoInteractions(operator);
  }
}
