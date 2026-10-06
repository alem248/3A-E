-- Datos de prueba (coordenadas aproximadas, solo para desarrollo)
INSERT INTO rol (nombre, descripcion) VALUES
 ('Poblador','Ciudadano que consulta el mapa y recibe alertas'),
 ('Defensa Civil','Gestiona alertas y zonas vulnerables'),
 ('Admin','Administración del sistema');

INSERT INTO nivel_riesgo (nombre, color_hex, descripcion) VALUES
 ('Verde','#2E7D32','Sin riesgo inmediato'),
 ('Amarillo','#F9A825','Precaución'),
 ('Naranja','#EF6C00','Riesgo alto'),
 ('Rojo','#C62828','Emergencia');

INSERT INTO evento_climatico (nombre, fecha_inicio, estado) VALUES
 ('Niño Costero 2026','2026-01-15','Activo');

INSERT INTO fuente_oficial (nombre_institucion, url_api, tipo_dato) VALUES
 ('SENAMHI','https://www.senamhi.gob.pe','Precipitación / Temperatura'),
 ('NOAA','https://www.noaa.gov','Temperatura superficial del mar'),
 ('IMARPE','https://www.imarpe.gob.pe','Oceanografía');

INSERT INTO estacion_monitoreo (fuente_id, nombre, tipo, latitud, longitud) VALUES
 (2,'Boya Talara','Boya',-4.580000,-81.270000),
 (2,'Boya Paita','Boya',-5.090000,-81.110000),
 (3,'Estación Pimentel','Estación Terrestre',-6.840000,-79.930000),
 (2,'Satélite Chimbote','Satélite',-9.070000,-78.600000),
 (1,'Estación Piura','Estación Terrestre',-5.200000,-80.630000);

-- Zonas (polígonos rectangulares de ejemplo, orden WKT: lon lat)
INSERT INTO zona_vulnerable (nombre_zona, nivel_riesgo_id, poligono) VALUES
 ('Ribera del río Piura',4, ST_GeomFromText('POLYGON((-80.70 -5.25,-80.58 -5.25,-80.58 -5.15,-80.70 -5.15,-80.70 -5.25))',4326)),
 ('Tumbes Centro',3,        ST_GeomFromText('POLYGON((-80.50 -3.62,-80.40 -3.62,-80.40 -3.52,-80.50 -3.52,-80.50 -3.62))',4326)),
 ('Chiclayo Sur',2,         ST_GeomFromText('POLYGON((-79.90 -6.80,-79.80 -6.80,-79.80 -6.70,-79.90 -6.70,-79.90 -6.80))',4326)),
 ('Chimbote Norte',1,       ST_GeomFromText('POLYGON((-78.62 -9.12,-78.52 -9.12,-78.52 -9.02,-78.62 -9.02,-78.62 -9.12))',4326));

INSERT INTO centro_ayuda (zona_vulnerable_id, nombre, tipo, latitud, longitud, aforo) VALUES
 (1,'Refugio Coliseo Piura','Refugio',-5.190000,-80.640000,800),
 (1,'Hospital Regional Piura','Hospital',-5.180000,-80.620000,300),
 (2,'Almacén Defensa Civil Tumbes','Almacén',-3.570000,-80.450000,0),
 (3,'Refugio Estadio Chiclayo','Refugio',-6.760000,-79.850000,1200);

-- TSM: lecturas recientes y anteriores por estación
INSERT INTO registro_tsm (evento_id, estacion_id, latitud, longitud, temperatura, anomalia, fecha_hora) VALUES
 (1,1,-4.580000,-81.270000,28.90,3.80, now() - interval '1 hour'),
 (1,1,-4.580000,-81.270000,28.60,3.50, now() - interval '7 hours'),
 (1,2,-5.090000,-81.110000,28.40,3.40, now() - interval '1 hour'),
 (1,2,-5.090000,-81.110000,28.10,3.10, now() - interval '7 hours'),
 (1,3,-6.840000,-79.930000,26.90,2.10, now() - interval '2 hours'),
 (1,4,-9.070000,-78.600000,25.10,1.20, now() - interval '3 hours');

INSERT INTO registro_precipitacion (evento_id, estacion_id, milimetros_lluvia, fecha_hora) VALUES
 (1,5,42.50, now() - interval '2 hours'),
 (1,5,18.00, now() - interval '8 hours'),
 (1,3,9.30,  now() - interval '3 hours');

INSERT INTO proyeccion_trayectoria (evento_id, lat_futura, lon_futura, fecha_estimada, confiabilidad_porcentaje) VALUES
 (1,-5.50,-81.00, current_date + 1, 85.00),
 (1,-6.00,-80.60, current_date + 2, 72.50),
 (1,-6.80,-80.10, current_date + 3, 58.00);

INSERT INTO alerta_preventiva (zona_vulnerable_id, nivel_riesgo_id, mensaje, vigencia_hasta) VALUES
 (1,4,'Riesgo de desborde del río Piura. Evacúe zonas ribereñas.', now() + interval '2 days'),
 (2,3,'Lluvias intensas previstas. Manténgase atento a comunicados.', now() + interval '1 day');

INSERT INTO medida_preventiva (alerta_id, titulo, descripcion_accion) VALUES
 (1,'Alejarse de la ribera','Aléjese de la ribera del río Piura y diríjase al refugio más cercano.'),
 (1,'Mochila de emergencia','Prepare agua, documentos, linterna y botiquín.'),
 (2,'Evitar quebradas','No cruce quebradas ni zonas inundables.');

INSERT INTO usuario (rol_id, nombres, email, password_hash, fcm_token) VALUES
 (1,'Usuario Demo','demo@ejemplo.com','$2a$10$hashDeEjemploNoUsarEnProduccion','fcm-token-demo');
INSERT INTO ubicacion_usuario (usuario_id, latitud, longitud) VALUES (1,-5.200000,-80.630000);
INSERT INTO historial_notificacion (usuario_id, alerta_id) VALUES (1,1);
