package cl.duoc.cuentas_service.service;

import cl.duoc.bank_common.*;
import cl.duoc.cuentas_service.repository.CuentaRepository;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

@Service
public class OperacionService {
  private final JdbcTemplate jdbc;
  private final CuentaRepository cuentas;
  private final ObjectMapper mapper;

  public OperacionService(JdbcTemplate j, CuentaRepository c, ObjectMapper m) {
    jdbc = j;
    cuentas = c;
    mapper = m;
  }

  @Transactional
  public OperacionDTO procesar(OperacionSolicitud s) {
    validar(s);
    var existing = find(s.idempotencia());
    if (existing.isPresent()) return repetir(existing.get(), s);
    Long origin = s.cuentaOrigen(), dest = s.cuentaDestino();
    var ids = new TreeSet<Long>();
    if (origin != null) ids.add(origin);
    if (dest != null) ids.add(dest);
    var locked = new HashMap<Long, CuentaDTO>();
    // Orden estable para transferencias cruzadas; el bloqueo dura toda la transacción.
    for (Long id : ids) {
      var rows = jdbc.query("SELECT * FROM cuentas WHERE id=? FOR UPDATE", cuentas::map, id);
      if (rows.isEmpty())
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta " + id + " no encontrada");
      if (!rows.getFirst().estado().equals("ACTIVA"))
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Cuenta cerrada");
      locked.put(id, rows.getFirst());
    }
    existing = find(s.idempotencia());
    if (existing.isPresent()) return repetir(existing.get(), s);
    BigDecimal before = locked.get(origin == null ? dest : origin).saldo();
    BigDecimal after = origin == null ? before.add(s.monto()) : before.subtract(s.monto());
    if (after.signum() < 0)
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Saldo insuficiente");
    if (origin != null) jdbc.update("UPDATE cuentas SET saldo=? WHERE id=?", after, origin);
    if (dest != null) jdbc.update("UPDATE cuentas SET saldo=saldo+? WHERE id=?", s.monto(), dest);
    jdbc.update(
        "INSERT INTO"
            + " operaciones(idempotencia,tipo,cuenta_origen,cuenta_destino,monto,referencia,saldo_anterior,saldo_posterior)"
            + " VALUES(?,?,?,?,?,?,?,?)",
        s.idempotencia(),
        s.tipo(),
        origin,
        dest,
        s.monto(),
        s.referencia(),
        before,
        after);
    OperacionDTO result = find(s.idempotencia()).orElseThrow();
    jdbc.update(
        "INSERT INTO eventos_pendientes(id,contenido) VALUES(?,?)",
        s.idempotencia(),
        mapper.writeValueAsString(result));
    return result;
  }

  public List<OperacionDTO> movimientos(Long id, int pagina, int limite) {
    return jdbc.query(
        "SELECT * FROM operaciones WHERE cuenta_origen=? OR cuenta_destino=? ORDER BY fecha"
            + " DESC,idempotencia DESC LIMIT ? OFFSET ?",
        this::map,
        id,
        id,
        limite,
        pagina * limite);
  }

  public Optional<OperacionDTO> find(String id) {
    return jdbc.query("SELECT * FROM operaciones WHERE idempotencia=?", this::map, id).stream()
        .findFirst();
  }

  private OperacionDTO repetir(OperacionDTO o, OperacionSolicitud s) {
    if (!Objects.equals(o.tipo(), s.tipo())
        || !Objects.equals(o.cuentaOrigen(), s.cuentaOrigen())
        || !Objects.equals(o.cuentaDestino(), s.cuentaDestino())
        || o.monto().compareTo(s.monto()) != 0
        || !Objects.equals(o.referencia(), s.referencia()))
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Idempotencia utilizada con otra solicitud");
    return o;
  }

  private void validar(OperacionSolicitud s) {
    if (s.idempotencia() == null || s.idempotencia().isBlank() || s.idempotencia().length() > 80)
      throw new IllegalArgumentException("Idempotencia requerida (máximo 80 caracteres)");
    if (s.monto() == null
        || s.monto().signum() <= 0
        || s.monto().scale() > 2
        || s.monto().compareTo(new BigDecimal("9999999999999.99")) > 0)
      throw new IllegalArgumentException("Monto inválido");
    if (s.tipo() == null) throw new IllegalArgumentException("Tipo requerido");
    boolean valid =
        switch (s.tipo()) {
          case "DEPOSITO" -> s.cuentaOrigen() == null && s.cuentaDestino() != null;
          case "TRANSFERENCIA" ->
              s.cuentaOrigen() != null
                  && s.cuentaDestino() != null
                  && !s.cuentaOrigen().equals(s.cuentaDestino());
          case "PAGO" ->
              s.cuentaOrigen() != null
                  && s.cuentaDestino() == null
                  && s.referencia() != null
                  && !s.referencia().isBlank();
          case "RETIRO" -> s.cuentaOrigen() != null && s.cuentaDestino() == null;
          default -> false;
        };
    if (!valid)
      throw new IllegalArgumentException(
          "Cuentas o referencia incompatibles con el tipo de operación");
  }

  private OperacionDTO map(java.sql.ResultSet r, int n) throws java.sql.SQLException {
    return new OperacionDTO(
        r.getString("idempotencia"),
        r.getString("tipo"),
        r.getObject("cuenta_origen", Long.class),
        r.getObject("cuenta_destino", Long.class),
        r.getBigDecimal("monto"),
        r.getString("referencia"),
        r.getBigDecimal("saldo_anterior"),
        r.getBigDecimal("saldo_posterior"),
        r.getString("estado"),
        r.getTimestamp("fecha").toLocalDateTime());
  }
}
