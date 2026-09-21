package cl.duoc.cuentas_service.repository;

import cl.duoc.cuentas_service.model.Cuenta;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public class CuentaRepository {

    private final List<Cuenta> cuentas = List.of(
        new Cuenta(201L, 101L, "corriente", new BigDecimal("450000")),
        new Cuenta(202L, 102L, "ahorro", new BigDecimal("1200000")),
        new Cuenta(203L, 103L, "corriente", new BigDecimal("87000")),
        new Cuenta(204L, 104L, "ahorro", new BigDecimal("3050000")),
        new Cuenta(205L, 105L, "corriente", new BigDecimal("15000"))
    );

    public List<Cuenta> findAll() { return cuentas; }

    public Optional<Cuenta> findById(Long id) {
        return cuentas.stream().filter(c -> c.getId().equals(id)).findFirst();
    }
}