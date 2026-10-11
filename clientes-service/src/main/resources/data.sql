INSERT INTO clientes(id,nombre,rut,email,perfil,estado) VALUES
(101,'Camila Rojas','12.345.678-9','camila.rojas@mail.cl','PERSONA','ACTIVO'),
(102,'Matías Fuentes','13.456.789-0','matias.fuentes@mail.cl','PERSONA','ACTIVO'),
(103,'Valentina Soto','14.567.890-1','valentina.soto@mail.cl','PERSONA','ACTIVO'),
(104,'Diego Herrera','15.678.901-2','diego.herrera@mail.cl','PERSONA','ACTIVO'),
(105,'Antonia Vega','16.789.012-3','antonia.vega@mail.cl','PERSONA','ACTIVO') ON CONFLICT(id) DO NOTHING;
