package cl.duoc.clientes_service.repository;

import cl.duoc.clientes_service.model.Cliente;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ClienteRepository {

    // Datos de ejemplo migrados desde bank_legacy_data
    private final List<Cliente> clientes = List.of(
        new Cliente(101L, "Camila Rojas", "12.345.678-9", "camila.rojas@mail.cl"),
        new Cliente(102L, "Matías Fuentes", "13.456.789-0", "matias.fuentes@mail.cl"),
        new Cliente(103L, "Valentina Soto", "14.567.890-1", "valentina.soto@mail.cl"),
        new Cliente(104L, "Diego Herrera", "15.678.901-2", "diego.herrera@mail.cl"),
        new Cliente(105L, "Antonia Vega", "16.789.012-3", "antonia.vega@mail.cl")
    );

    public List<Cliente> findAll() {
        return clientes;
    }

    public Optional<Cliente> findById(Long id) {
        return clientes.stream().filter(c -> c.getId().equals(id)).findFirst();
    }
}