package cl.duoc.pagos_service;

import cl.duoc.bank_common.RemoteServices;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(RemoteServices.class)
public class PagosServiceApplication {
  public static void main(String[] args) {
    SpringApplication.run(PagosServiceApplication.class, args);
  }
}
