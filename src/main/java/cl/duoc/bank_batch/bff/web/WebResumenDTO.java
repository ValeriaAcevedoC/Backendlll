package cl.duoc.bank_batch.bff.web;

public record WebResumenDTO(
        String canal,
        String descripcion,
        Resumen resumen
) {

    public record Resumen(
            Long transaccionesProcesadas,
            Long resumenesDiarios,
            Long cuentasConIntereses,
            Long resumenesAnuales
    ) {
    }
}