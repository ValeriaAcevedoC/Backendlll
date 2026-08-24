CREATE TABLE IF NOT EXISTS transacciones_procesadas (
    id BIGINT PRIMARY KEY,
    fecha DATE,
    monto NUMERIC(15,2),
    tipo VARCHAR(20),
    anomalia BOOLEAN,
    detalle_anomalia VARCHAR(255)
);

ALTER TABLE transacciones_procesadas
ADD COLUMN IF NOT EXISTS anomalia BOOLEAN;

ALTER TABLE transacciones_procesadas
ADD COLUMN IF NOT EXISTS detalle_anomalia VARCHAR(255);

CREATE TABLE IF NOT EXISTS cuentas_intereses (
    cuenta_id BIGINT PRIMARY KEY,
    nombre VARCHAR(100),
    saldo NUMERIC(15,2),
    edad INTEGER,
    tipo VARCHAR(30),
    tasa_interes NUMERIC(10,4),
    interes_calculado NUMERIC(15,2),
    saldo_final NUMERIC(15,2),
    valida BOOLEAN,
    observacion VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS movimientos_anuales (
    cuenta_id BIGINT,
    fecha DATE,
    transaccion VARCHAR(30),
    monto NUMERIC(15,2),
    descripcion VARCHAR(255),
    anomalia BOOLEAN,
    detalle_anomalia VARCHAR(255),
    PRIMARY KEY (cuenta_id, fecha, transaccion, monto)
);

CREATE TABLE IF NOT EXISTS resumen_anual (
    cuenta_id BIGINT PRIMARY KEY,
    total_movimientos INTEGER,
    total_ingresos NUMERIC(15,2),
    total_egresos NUMERIC(15,2),
    saldo_anual NUMERIC(15,2),
    total_anomalias INTEGER
);