-- =====================================================================
-- V19 — Numero de plaza + planta derivada (change parking-space-floors).
-- Anade la columna entera dedicada `number` (>= 1000) a parking_spaces, de la
-- que se DERIVA la planta en el dominio (floor = number / 1000): 1001..1999 ->
-- planta 1, 2001..2999 -> planta 2, etc. La planta NO se persiste (design §Dec.1).
--
-- Renumera las plazas existentes por orden de `id` (preservando el id, y por
-- tanto todas las FK polimorficas y reales por id): para la fila k-esima (0-based)
-- con spacesPerFloor = 5 -> floor = 1 + (k / 5); number = floor*1000 + (k % 5) + 1;
-- label = CONVERT(NVARCHAR, number). Con 25 plazas quedan 1001-1005, 2001-2005,
-- 3001-3005, 4001-4005, 5001-5005 (design §Dec.3).
--
-- El indice unico UX_parking_spaces_number garantiza la unicidad de `number`
-- frente a concurrencia (segunda insercion -> 409 en la capa de app), en linea
-- con UX_parking_spaces_label de V6.
--
-- Nota de versionado: V14 y V16-V18 estan reservadas por seeds de desarrollo
-- (db/seed/dev, solo perfil des); esta migracion de ESQUEMA usa V19 para no
-- colisionar con esas versiones en el perfil des.
-- =====================================================================

ALTER TABLE dbo.parking_spaces ADD number INT NULL;
GO

ALTER TABLE dbo.parking_spaces
    ADD CONSTRAINT CK_parking_spaces_number CHECK (number >= 1000);
GO

-- Renumeracion 1000-based por orden de id (spacesPerFloor = 5), sin tocar el id.
WITH ordered AS (
    SELECT id, (ROW_NUMBER() OVER (ORDER BY id) - 1) AS k
    FROM dbo.parking_spaces
)
UPDATE ps
SET number = (1 + (o.k / 5)) * 1000 + (o.k % 5) + 1,
    label  = CONVERT(NVARCHAR(20), (1 + (o.k / 5)) * 1000 + (o.k % 5) + 1)
FROM dbo.parking_spaces ps
INNER JOIN ordered o ON o.id = ps.id;
GO

ALTER TABLE dbo.parking_spaces ALTER COLUMN number INT NOT NULL;
GO

CREATE UNIQUE INDEX UX_parking_spaces_number ON dbo.parking_spaces(number);
GO
