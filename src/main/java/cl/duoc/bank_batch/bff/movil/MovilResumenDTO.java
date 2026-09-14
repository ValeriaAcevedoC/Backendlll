package cl.duoc.bank_batch.bff.movil;

public record MovilResumenDTO(
        String canal,
        Long transacciones,
        Long resumenesDiarios
) {
}