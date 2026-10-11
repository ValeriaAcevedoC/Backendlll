package cl.duoc.bank_batch;

import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BankBatchApplication {
  public static void main(String[] args) {
    SpringApplication.run(BankBatchApplication.class, args);
  }
}
