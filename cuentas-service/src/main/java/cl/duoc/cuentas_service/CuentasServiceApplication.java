package cl.duoc.cuentas_service;

import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CuentasServiceApplication {
  public static void main(String[] args) {
    SpringApplication.run(CuentasServiceApplication.class, args);
  }
}
