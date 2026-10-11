INSERT INTO cuentas(id,cliente_id,tipo,saldo,estado) VALUES
(201,101,'corriente',450000,'ACTIVA'),(202,102,'ahorro',1200000,'ACTIVA'),(203,103,'corriente',87000,'ACTIVA'),
(204,104,'ahorro',3050000,'ACTIVA'),(205,105,'corriente',15000,'ACTIVA') ON CONFLICT(id) DO NOTHING;
