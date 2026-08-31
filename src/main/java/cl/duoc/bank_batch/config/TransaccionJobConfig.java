package cl.duoc.bank_batch.config;

import cl.duoc.bank_batch.model.Transaccion;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.infrastructure.item.support.SynchronizedItemStreamReader;
import org.springframework.batch.infrastructure.item.support.builder.SynchronizedItemStreamReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.TransientDataAccessException;
import javax.sql.DataSource;

import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.transaction.PlatformTransactionManager;

import cl.duoc.bank_batch.processor.TransaccionProcessor;
import cl.duoc.bank_batch.policy.CustomSkipPolicy;
import cl.duoc.bank_batch.policy.DataQualityDecider;
import org.springframework.core.task.AsyncTaskExecutor;

@Configuration
public class TransaccionJobConfig {

    @Bean
    public SynchronizedItemStreamReader<Transaccion> transaccionReader() {

        FlatFileItemReader<Transaccion> delegate = new FlatFileItemReaderBuilder<Transaccion>()
                .name("transaccionReader")
                .resource(new ClassPathResource("data/transacciones.csv"))
                .linesToSkip(1)
                .delimited()
                .names("id", "fecha", "monto", "tipo")
                .targetType(Transaccion.class)
                .build();

        return new SynchronizedItemStreamReaderBuilder<Transaccion>()
                .delegate(delegate)
                .build();
    }


    @Bean
    public JdbcBatchItemWriter<Transaccion> transaccionWriter(DataSource dataSource) {

        return new JdbcBatchItemWriterBuilder<Transaccion>()
                .dataSource(dataSource)
                .sql("""
                    INSERT INTO transacciones_procesadas
                    (id, fecha, monto, tipo, anomalia, detalle_anomalia)
                    VALUES (
                        :id,
                        TO_DATE(:fecha, 'YYYY-MM-DD'),
                        :monto,
                        :tipo,
                        :anomalia,
                        :detalleAnomalia
                    )
                    ON CONFLICT (id) DO UPDATE SET
                        fecha = EXCLUDED.fecha,
                        monto = EXCLUDED.monto,
                        tipo = EXCLUDED.tipo,
                        anomalia = EXCLUDED.anomalia,
                        detalle_anomalia = EXCLUDED.detalle_anomalia
                    """)
                .beanMapped()
                .build();
    }

    @Bean
    public Step transaccionStep(
        JobRepository jobRepository,
        PlatformTransactionManager transactionManager,
        SynchronizedItemStreamReader<Transaccion> transaccionReader,
        TransaccionProcessor transaccionProcessor,
        JdbcBatchItemWriter<Transaccion> transaccionWriter,
        CustomSkipPolicy customSkipPolicy,
        AsyncTaskExecutor taskExecutor) {

        return new StepBuilder("transaccionStep", jobRepository)
            .<Transaccion, Transaccion>chunk(5)
            .transactionManager(transactionManager)
            .reader(transaccionReader)
            .processor(transaccionProcessor)
            .writer(transaccionWriter)
            .faultTolerant()
            .retry(TransientDataAccessException.class)
            .retryLimit(3)
            .skipPolicy(customSkipPolicy)
            .taskExecutor(taskExecutor)
            .build();
    }

    @Bean
    public Job transaccionJob(
        JobRepository jobRepository,
        Step transaccionStep,
        DataQualityDecider dataQualityDecider) {

        return new JobBuilder("transaccionJob", jobRepository)
            .start(transaccionStep)
            .next(dataQualityDecider).on(DataQualityDecider.CALIDAD_INSUFICIENTE).fail()
            .from(dataQualityDecider).on("*").end()
            .end()
            .build();
    }
}
