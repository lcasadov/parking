-- =====================================================================
-- V17 — Seed empleado de DESARROLLO (idempotente).
-- Inserta un unico EMPLOYEE local para poder probar los flujos de solicitud
-- (plaza/puesto) en LOCAL/DES y en los tests e2e (Puerta 3). NO es un usuario
-- de PRODUCCION. Solo perfil `des` (esta migracion vive en db/seed/dev, que
-- solo se carga desde application-des.yml; los ITs no la aplican).
--
-- Credenciales (documentadas en e2e/README.md):
--   login    : empleado
--   password : Empleado#Dev2026   (>=10, may+min+digito+simbolo, != login/email)
--   email    : empleado.dev@aleatica.local
--   role     : EMPLOYEE
-- El password_hash es BCrypt(coste 12, variante $2a$) de la contrasena anterior,
-- generado y verificado con una util bcrypt (cross-check contra el hash del admin
-- V5 para confirmar compatibilidad con el BCryptPasswordEncoder de Spring).
-- password_must_change = 0: es empleado de desarrollo, no fruto de un reset, para
-- poder loguearse directo en e2e sin forzar el cambio de contrasena.
-- =====================================================================

IF NOT EXISTS (SELECT 1 FROM dbo.employees WHERE login = N'empleado')
BEGIN
    INSERT INTO dbo.employees
        (first_name, last_name, login, email, password_hash, password_must_change,
         is_corporate, auth_origin, role, enabled, active, last_password_change_at, created_at)
    VALUES
        (N'Dev', N'Empleado', N'empleado', N'empleado.dev@aleatica.local',
         '$2a$12$hYwO2S./Uwts0tdG1Lqm5uXx0bI3Zfv1GBI0LRW87aOSrnwFsjjz6', 0,
         0, 'LOCAL', 'EMPLOYEE', 1, 1, SYSUTCDATETIME(), SYSUTCDATETIME());
END;
GO
