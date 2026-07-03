-- =====================================================================
-- V5 — Seed admin de DESARROLLO (idempotente).
-- Inserta un unico administrador local para poder probar el login real en
-- LOCAL/DES y en los tests e2e (Puerta 3). NO es el admin de PRODUCCION.
--
-- Credenciales (documentadas en proposal.md §"Seed admin de desarrollo"):
--   login    : admin
--   password : Admin#Parking2026   (>=10, may+min+digito+simbolo, != login/email)
--   email    : admin.dev@aleatica.local
--   role     : ADMIN
-- El password_hash es BCrypt(coste 12) de la contrasena anterior.
-- password_must_change = 0: es admin de desarrollo, no fruto de un reset.
-- =====================================================================

IF NOT EXISTS (SELECT 1 FROM dbo.employees WHERE login = N'admin')
BEGIN
    INSERT INTO dbo.employees
        (first_name, last_name, login, email, password_hash, password_must_change,
         is_corporate, auth_origin, role, enabled, active, last_password_change_at, created_at)
    VALUES
        (N'Dev', N'Admin', N'admin', N'admin.dev@aleatica.local',
         '$2a$12$xFfFx12kQw6hOFBT8E3NKe.GZO0pR5ncDvi3w/xKNdEIED66N4a/i', 0,
         0, 'LOCAL', 'ADMIN', 1, 1, SYSUTCDATETIME(), SYSUTCDATETIME());
END;
GO
