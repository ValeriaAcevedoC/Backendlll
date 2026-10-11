package cl.duoc.bank_batch;

import static org.junit.jupiter.api.Assertions.*;

import cl.duoc.bank_batch.model.*;
import cl.duoc.bank_batch.processor.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ProcessorTests {
  @Test
  void interesMensualYAhorro() {
    var c = cuenta("ahorro");
    var r = new InteresProcessor().process(c);
    assertEquals(new BigDecimal("10.00"), r.getInteresCalculado());
    assertEquals(new BigDecimal("1010.00"), r.getSaldoFinal());
  }

  @Test
  void interesMensualPrestamo() {
    var r = new InteresProcessor().process(cuenta("prestamo"));
    assertEquals(new BigDecimal("20.00"), r.getInteresCalculado());
  }

  @Test
  void retiroPositivoNoEsIngreso() {
    var m = new MovimientoAnual();
    m.setCuentaId(101L);
    m.setFecha("15/06/2024");
    m.setTransaccion("retiro");
    m.setMonto(new BigDecimal("50"));
    m.setDescripcion("Retiro");
    var r = new MovimientoAnualProcessor().process(m);
    assertFalse(r.isAnomalia());
    assertEquals("retiro", r.getTransaccion());
    assertEquals("2024-06-15", r.getFecha());
  }

  @Test
  void montoNegativoSeMarca() {
    var m = new MovimientoAnual();
    m.setCuentaId(101L);
    m.setFecha("2024-06-15");
    m.setTransaccion("deposito");
    m.setMonto(new BigDecimal("-50"));
    m.setDescripcion("Depósito");
    assertTrue(new MovimientoAnualProcessor().process(m).isAnomalia());
  }

  private CuentaInteres cuenta(String tipo) {
    var c = new CuentaInteres();
    c.setCuentaId(101L);
    c.setNombre("Camila");
    c.setEdad(30);
    c.setSaldo(new BigDecimal("1000"));
    c.setTipo(tipo);
    return c;
  }
}
