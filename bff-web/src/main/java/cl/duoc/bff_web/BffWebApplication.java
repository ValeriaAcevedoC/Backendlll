package cl.duoc.bff_web;

import cl.duoc.bank_common.RemoteServices;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(RemoteServices.class)
public class BffWebApplication {
  public static void main(String[] args) {
    SpringApplication.run(BffWebApplication.class, args);
  }
}
