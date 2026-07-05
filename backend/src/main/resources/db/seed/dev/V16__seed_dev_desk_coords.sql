-- =====================================================================
-- V16 — Reparto de coordenadas de DESARROLLO de los 65 puestos (idempotente).
--
-- V14 sembró los 65 puestos en la coordenada central (50,50), lo que los
-- renderiza APILADOS e ilegibles en el plano (floor-plan). Esta migración los
-- redistribuye en una rejilla determinista de 9 columnas para que el plano sea
-- legible de partida, sin depender de que el ADMIN recoloque 65 marcadores a
-- mano.
--
-- Rejilla: para el puesto n (1..65), idx = n-1
--   columna = idx % 9        (0..8)
--   fila    = idx / 9        (0..7, división entera T-SQL)
--   coord_x = 10 + columna*10 (10..90)  → dentro de 0..100
--   coord_y = 10 + fila*11    (10..87)  → dentro de 0..100
--
-- Idempotente: es un único UPDATE que asigna siempre el mismo valor por número.
-- Solo perfil des (classpath:db/seed/dev). No afecta a PRE/PRO (tabla vacía) ni
-- a los ITs (BaseIntegrationTest#resetDomainState vacía desks antes de cada test).
-- =====================================================================

UPDATE d
SET coord_x = 10 + ((d.number - 1) % 9) * 10,
    coord_y = 10 + ((d.number - 1) / 9) * 11
FROM dbo.desks d
WHERE d.number BETWEEN 1 AND 65;
GO
