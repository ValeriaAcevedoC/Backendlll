package cl.duoc.clientes_service.model;

public record Cliente(
    Long id, String nombre, String rut, String email, String perfil, String estado) {}
