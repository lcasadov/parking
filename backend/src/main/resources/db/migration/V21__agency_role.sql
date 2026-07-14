-- =====================================================================
-- V21 — Rol AGENCIA (change funcional agency-role).
-- Amplia el CHECK CK_employees_role para admitir el nuevo valor 'AGENCIA',
-- rol de minimo privilegio cuya unica capacidad es la liberacion
-- administrativa. Debe desplegarse ANTES de crear usuarios con ese rol.
-- Alineado con docs/data-model.md §3.1 y el enum
-- com.aleatica.parking.employee.Role. Estilo identico a V4__employees.sql:
-- se reemplaza el CHECK (DROP + ADD) con el nuevo conjunto de valores.
-- =====================================================================

ALTER TABLE dbo.employees DROP CONSTRAINT CK_employees_role;
GO

ALTER TABLE dbo.employees
    ADD CONSTRAINT CK_employees_role CHECK (role IN ('ADMIN','EMPLOYEE','AGENCIA'));
GO
