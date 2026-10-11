package cl.duoc.clientes_service.repository;

import cl.duoc.clientes_service.model.Cliente;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

@Repository
public class ClienteRepository {
  private final JdbcTemplate jdbc;

  public ClienteRepository(JdbcTemplate j) {
    jdbc = j;
  }

  public List<Cliente> findAll() {
    return jdbc.query("SELECT * FROM clientes ORDER BY id", this::map);
  }

  public Optional<Cliente> findById(Long id) {
    return jdbc.query("SELECT * FROM clientes WHERE id=?", this::map, id).stream().findFirst();
  }

  public Cliente crear(String n, String r, String e, String p) {
    var key = new org.springframework.jdbc.support.GeneratedKeyHolder();
    jdbc.update(
        c -> {
          var s =
              c.prepareStatement(
                  "INSERT INTO clientes(nombre,rut,email,perfil,estado) VALUES(?,?,?,?,'ACTIVO')",
                  new String[] {"id"});
          s.setString(1, n.trim());
          s.setString(2, r.trim());
          s.setString(3, e.trim());
          s.setString(4, p);
          return s;
        },
        key);
    return findById(key.getKey().longValue()).orElseThrow();
  }

  public Cliente actualizar(Long id, String n, String e, String p) {
    if (jdbc.update(
            "UPDATE clientes SET"
                + " nombre=COALESCE(?,nombre),email=COALESCE(?,email),perfil=COALESCE(?,perfil)"
                + " WHERE id=?",
            n,
            e,
            p,
            id)
        == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado");
    return findById(id).orElseThrow();
  }

  private Cliente map(java.sql.ResultSet r, int n) throws java.sql.SQLException {
    return new Cliente(
        r.getLong("id"),
        r.getString("nombre"),
        r.getString("rut"),
        r.getString("email"),
        r.getString("perfil"),
        r.getString("estado"));
  }
}
