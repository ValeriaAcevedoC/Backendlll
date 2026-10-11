package cl.duoc.bank_batch;

import static org.junit.jupiter.api.Assertions.*;

import cl.duoc.bank_batch.service.BatchOperations;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.*;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.transaction.PlatformTransactionManager;

@SpringBootTest(
    classes = {BankBatchApplication.class, BatchRecoveryIntegrationTests.ProbeConfig.class})
class BatchRecoveryIntegrationTests {
  @Autowired JobOperator operator;
  @Autowired BatchOperations recovery;
  @Autowired JobRepository repository;

  @Autowired
  @Qualifier("restartProbeJob")
  Job job;

  @Test
  void recuperaInstanciaFallidaUsandoMetadatosJdbc() throws Exception {
    var params =
        new JobParametersBuilder()
            .addString("ejecucion", UUID.randomUUID().toString())
            .toJobParameters();
    var first = operator.start(job, params);
    assertEquals(BatchStatus.FAILED, first.getStatus());
    recovery.recuperar();
    var last = repository.getLastJobExecution(first.getJobInstance());
    assertNotNull(last);
    assertEquals(BatchStatus.COMPLETED, last.getStatus());
    assertNotEquals(first.getId(), last.getId());
    assertEquals(2, repository.getJobExecutions(first.getJobInstance()).size());
  }

  @TestConfiguration
  static class ProbeConfig {
    @Bean
    Job restartProbeJob(JobRepository repository, PlatformTransactionManager transactionManager) {
      var first = new AtomicBoolean(true);
      var step =
          new StepBuilder("restartProbeStep", repository)
              .tasklet(
                  (c, ctx) -> {
                    if (first.getAndSet(false))
                      throw new TransientDataAccessResourceException("Fallo temporal simulado");
                    return RepeatStatus.FINISHED;
                  },
                  transactionManager)
              .build();
      return new JobBuilder("restartProbeJob", repository).start(step).build();
    }
  }
}
