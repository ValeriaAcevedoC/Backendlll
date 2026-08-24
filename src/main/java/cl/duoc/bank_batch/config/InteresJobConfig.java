package cl.duoc.bank_batch.config;

import cl.duoc.bank_batch.model.CuentaInteres;

import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.sql.DataSource;

import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;
import cl.duoc.bank_batch.processor.InteresProcessor;
import cl.duoc.bank_batch.policy.CustomSkipPolicy;
import org.springframework.core.task.AsyncTaskExecutor;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class InteresJobConfig {

    @Bean
    public FlatFileItemReader<CuentaInteres> interesReader() {

        return new FlatFileItemReaderBuilder<CuentaInteres>()
                .name("interesReader")
                .resource(new ClassPathResource("data/intereses.csv"))
                .linesToSkip(1)
                .delimited()
                .names("cuentaId", "nombre", "saldo", "edad", "tipo")
                .targetType(CuentaInteres.class)
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<CuentaInteres> interesWriter(DataSource dataSource) {

        return new JdbcBatchItemWriterBuilder<CuentaInteres>()
                .dataSource(dataSource)
                .sql("""
                    INSERT INTO cuentas_intereses (
                        cuenta_id,
                        nombre,
                        saldo,
                        edad,
                        tipo,
                        tasa_interes,
                        interes_calculado,
                        saldo_final,
                        valida,
                        observacion
                    )
                    VALUES (
                        :cuentaId,
                        :nombre,
                        :saldo,
                        :edad,
                        :tipo,
                        :tasaInteres,
                        :interesCalculado,
                        :saldoFinal,
                        :valida,
                        :observacion
                    )
                    ON CONFLICT (cuenta_id) DO UPDATE SET
                        nombre = EXCLUDED.nombre,
                        saldo = EXCLUDED.saldo,
                        edad = EXCLUDED.edad,
                        tipo = EXCLUDED.tipo,
                        tasa_interes = EXCLUDED.tasa_interes,
                        interes_calculado = EXCLUDED.interes_calculado,
                        saldo_final = EXCLUDED.saldo_final,
                        valida = EXCLUDED.valida,
                        observacion = EXCLUDED.observacion
                    """)
                .beanMapped()
                .build();
    }

   @Bean
    public Step interesStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            FlatFileItemReader<CuentaInteres> interesReader,
            InteresProcessor interesProcessor,
            JdbcBatchItemWriter<CuentaInteres> interesWriter,
            CustomSkipPolicy customSkipPolicy,
            AsyncTaskExecutor taskExecutor) {
                
        return new StepBuilder("interesStep", jobRepository)
                .<CuentaInteres, CuentaInteres>chunk(5)
                .transactionManager(transactionManager)
                .reader(interesReader)
                .processor(interesProcessor)
                .writer(interesWriter)
                .faultTolerant()
                .skipPolicy(customSkipPolicy)
                .taskExecutor(taskExecutor)
                .build();
    }

    @Bean
    public Job interesJob(
            JobRepository jobRepository,
            Step interesStep) {

        return new JobBuilder("interesJob", jobRepository)
                .start(interesStep)
                .build();
    }
}