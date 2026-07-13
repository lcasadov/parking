-- =====================================================================
-- V20 — Categoria jerarquica del empleado (change employee-category).
-- Anade la columna `category` (rango organizativo) al catalogo de empleados.
-- Dominio cerrado de 7 valores validado con CHECK, alineado con el patron de
-- `role`/`auth_origin` (ver V4__employees.sql). El default `EMPLEADO` puebla las
-- filas existentes (backfill conservador: menor rango) y satisface NOT NULL sin
-- necesidad de datos previos; el alta via API siempre envia un valor explicito.
-- =====================================================================

ALTER TABLE dbo.employees
    ADD category VARCHAR(20) NOT NULL
        CONSTRAINT DF_employees_category DEFAULT 'EMPLEADO';
GO

ALTER TABLE dbo.employees
    ADD CONSTRAINT CK_employees_category
        CHECK (category IN ('CEO','CONSEJO','DIRECTOR_N1','DIRECTOR_N2',
                            'GERENTE','MANDO_INTERMEDIO','EMPLEADO'));
GO
