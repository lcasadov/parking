-- =====================================================================
-- V14 — Seed de DESARROLLO de los 65 puestos (idempotente).
--
-- Inserta los 65 puestos STANDARD numerados con coordenadas neutras
-- (centro del plano, 50/50) para poder probar el ciclo de reserva de
-- puesto en LOCAL/DES. Solo perfil des (classpath:db/seed/dev): PRE/PRO
-- arrancan con la tabla vacia y el ADMIN da de alta los puestos via CRUD.
--
-- Coherente con el patron del seed admin de desarrollo (V5). Los ITs
-- (perfil des) parten de este seed pero BaseIntegrationTest#resetDomainState
-- vacia la tabla desks antes de cada test, de modo que las pruebas de alta
-- de puesto operan sobre una tabla limpia.
-- =====================================================================

WITH numbers AS (
    SELECT TOP (65) ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS n
    FROM sys.all_objects
)
INSERT INTO dbo.desks (number, category, coord_x, coord_y, active)
SELECT n, 'STANDARD', 50, 50, 1
FROM numbers
WHERE NOT EXISTS (SELECT 1 FROM dbo.desks d WHERE d.number = numbers.n);
GO
