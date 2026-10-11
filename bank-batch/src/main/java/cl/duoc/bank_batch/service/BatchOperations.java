package cl.duoc.bank_batch.service;

import java.time.YearMonth;
import java.util.*;
import org.slf4j.*;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.*;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BatchOperations {
  private static final Logger log = LoggerFactory.getLogger(BatchOperations.class);
  private final Map<String, Job> jobs = new HashMap<>();
  private final JobOperator operator;
  private final JobRepository repository;
  private final int maxIntentos;

  public BatchOperations(
      List<Job> all,
      JobOperator o,
      JobRepository r,
      @Value("${batch.restart.max-attempts:3}") int max) {
    all.forEach(j -> jobs.put(j.getName(), j));
    operator = o;
    repository = r;
    maxIntentos = max;
  }

  public synchronized Map<String, Object> iniciar(String nombre, String periodo, String ejecucion)
      throws Exception {
    Job job = jobs.get(nombre);
    if (job == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Job no encontrado");
    YearMonth.parse(periodo);
    if (ejecucion == null || ejecucion.isBlank() || ejecucion.length() > 80)
      throw new IllegalArgumentException("Identificador de ejecución requerido");
    if (!repository.findRunningJobExecutions(nombre).isEmpty())
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Job en ejecución");
    String archivo =
        switch (nombre) {
          case "transaccionJob" -> "transacciones.csv";
          case "interesJob" -> "intereses.csv";
          case "estadoCuentaJob" -> "cuentas_anuales.csv";
          default -> throw new IllegalArgumentException("Job inválido");
        };
    try {
      var p =
          new JobParametersBuilder()
              .addString("periodo", periodo)
              .addString("archivo", "classpath:data/" + archivo)
              .addString("ejecucion", ejecucion)
              .toJobParameters();
      return resumen(operator.start(job, p));
    } catch (org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException e) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Esta ejecución ya terminó; use otro identificador para reprocesar",
          e);
    }
  }

  public Map<String, Object> consultar(Long id) {
    var e = repository.getJobExecution(id);
    if (e == null)
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ejecución no encontrada");
    return resumen(e);
  }

  public Map<String, Object> resumen(JobExecution e) {
    return Map.of(
        "id",
        e.getId(),
        "job",
        e.getJobInstance().getJobName(),
        "estado",
        e.getStatus().name(),
        "salida",
        e.getExitStatus().getExitCode(),
        "steps",
        e.getStepExecutions().stream()
            .map(
                s ->
                    Map.of(
                        "nombre",
                        s.getStepName(),
                        "leidos",
                        s.getReadCount(),
                        "escritos",
                        s.getWriteCount(),
                        "omitidos",
                        s.getSkipCount(),
                        "inicio",
                        String.valueOf(s.getStartTime()),
                        "fin",
                        String.valueOf(s.getEndTime())))
            .toList());
  }

  @Scheduled(
      fixedDelayString = "${batch.restart.delay-ms:30000}",
      initialDelayString = "${batch.restart.delay-ms:30000}")
  public synchronized void recuperar() {
    for (String name : jobs.keySet()) {
      // Estado y número de intentos se conservan en el JobRepository.
      for (var instance : repository.getJobInstances(name, 0, 100)) {
        var history = repository.getJobExecutions(instance);
        if (history.isEmpty()) continue;
        var last = history.stream().max(Comparator.comparing(JobExecution::getId)).orElseThrow();
        if (last.getStatus() != BatchStatus.FAILED
            || history.size() >= maxIntentos
            || !temporal(last)) continue;
        try {
          var restarted = operator.restart(last);
          log.info("Job {} reiniciado: {}", name, restarted.getId());
        } catch (Exception e) {
          log.warn("No se pudo reiniciar {}: {}", last.getId(), e.getMessage());
        }
      }
    }
  }

  private boolean temporal(JobExecution e) {
    if (e.getExitStatus().getExitCode().contains("CALIDAD_INSUFICIENTE")) return false;
    String errors =
        e.getExitStatus().getExitDescription()
            + e.getStepExecutions().stream()
                .map(s -> s.getExitStatus().getExitDescription())
                .reduce("", String::concat);
    return errors.contains("TransientDataAccess")
        || errors.contains("CannotGetJdbcConnection")
        || errors.contains("SQLTransient")
        || e.getAllFailureExceptions().stream().anyMatch(this::causaTemporal);
  }

  private boolean causaTemporal(Throwable t) {
    for (Throwable c = t; c != null; c = c.getCause())
      if (c instanceof TransientDataAccessException
          || c instanceof org.springframework.jdbc.CannotGetJdbcConnectionException
          || c instanceof java.sql.SQLTransientException) return true;
    return false;
  }
}
