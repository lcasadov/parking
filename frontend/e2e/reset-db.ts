import { execFileSync } from 'node:child_process';

// =====================================================================
// Aislamiento e2e (Puerta 3): restablece la BD dev a su baseline de seeds
// ANTES DE CADA test (fixture auto-use en `fixtures.ts`).
//
// Motivo: la BD dev es compartida y mutable; `cancelEmployeePending` cancela
// pero no borra, y varios journeys (releases, visitantes, CRUD, cambio de
// contraseña) crean filas y empleados que, acumulados, rompen asserts paginadas,
// el guard de duplicados y la unicidad de login/email. Resetear una vez
// (globalSetup) no basta en modo --ui (re-lanzar no re-ejecuta el setup global).
//
// Estrategia: vaciar las tablas mutables (ningún seed las puebla) y eliminar los
// empleados/plazas creados por los tests, conservando SOLO los seeds
// (admin/emp7210/empleado y las 25 plazas + 65 puestos base). Las plazas creadas
// por tests se marcan con prefijo `E2E-` en la etiqueta.
//
// Configurable por entorno (para no acoplarlo a esta máquina):
//   E2E_DB_CONTAINER  contenedor docker SQL Server (def. parking-sqlserver)
//   MSSQL_SA_PASSWORD contraseña de sa              (def. Parking!Local2024)
//   E2E_DB_NAME       base de datos                 (def. parking)
//   E2E_SKIP_DB_RESET '1' para omitir (p. ej. sin docker)
// =====================================================================

const CONTAINER = process.env.E2E_DB_CONTAINER ?? 'parking-sqlserver';
const SA_PASSWORD = process.env.MSSQL_SA_PASSWORD ?? 'Parking!Local2024';
const DB_NAME = process.env.E2E_DB_NAME ?? 'parking';
const SQLCMD = '/opt/mssql-tools18/bin/sqlcmd';

// Logins de los seeds que NUNCA se borran.
const SEED_LOGINS = "'admin','emp7210','empleado'";
const SEED_IDS = `(SELECT id FROM employees WHERE login IN (${SEED_LOGINS}))`;

// Orden respetando FKs: hijos antes que padres. QUOTED_IDENTIFIER ON es
// obligatorio (índices filtrados en `requests`).
const RESET_SQL = [
  'SET QUOTED_IDENTIFIER ON',
  'SET NOCOUNT ON',
  'DELETE FROM visitor_reservations',
  'DELETE FROM visitors',
  'DELETE FROM releases',
  'DELETE FROM requests',
  'DELETE FROM fixed_assignments',
  `DELETE FROM login_log WHERE employee_id NOT IN ${SEED_IDS}`,
  `DELETE FROM audit_log WHERE actor_employee_id NOT IN ${SEED_IDS}`,
  `DELETE FROM employees WHERE login NOT IN (${SEED_LOGINS})`,
  `UPDATE employees SET password_must_change = 0, failed_login_attempts = 0, locked_until = NULL WHERE login IN (${SEED_LOGINS})`,
  "DELETE FROM parking_spaces WHERE label LIKE 'E2E-%'",
  // Restaura el estado activo de los recursos seed (el journey de puestos hace
  // toggle desactivar/reactivar; asegura un baseline determinista).
  "UPDATE parking_spaces SET active = 1 WHERE label NOT LIKE 'E2E-%'",
  'UPDATE desks SET active = 1',
].join('; ');

// Inserta un empleado EMPLOYEE con passwordMustChange=1 copiando el hash de
// `empleado` (=> su contraseña es la misma: Empleado#Dev2026), con login/email
// dedicados. Lo usa el journey de cambio de contraseña obligatorio sin tocar los
// usuarios seed. `resetToBaseline` lo elimina al ser no-seed.
const MUST_CHANGE_LOGIN = 'e2e_mustchg';
const MUST_CHANGE_PASSWORD = 'Empleado#Dev2026';
const SEED_MUST_CHANGE_SQL = [
  'SET QUOTED_IDENTIFIER ON',
  'SET NOCOUNT ON',
  `DELETE FROM login_log WHERE employee_id IN (SELECT id FROM employees WHERE login='${MUST_CHANGE_LOGIN}')`,
  `DELETE FROM employees WHERE login='${MUST_CHANGE_LOGIN}'`,
  'INSERT INTO employees (first_name,last_name,login,email,password_hash,password_must_change,' +
    'is_corporate,auth_origin,role,enabled,active,failed_login_attempts,last_password_change_at,created_at) ' +
    `SELECT first_name,'Chg','${MUST_CHANGE_LOGIN}','${MUST_CHANGE_LOGIN}@test.local',password_hash,1,` +
    "is_corporate,auth_origin,'EMPLOYEE',1,1,0,last_password_change_at,created_at " +
    "FROM employees WHERE login='empleado'",
].join('; ');

function runSql(sql: string): void {
  execFileSync(
    'docker',
    ['exec', CONTAINER, SQLCMD, '-S', 'localhost', '-U', 'sa', '-P', SA_PASSWORD, '-C', '-b', '-d', DB_NAME, '-Q', sql],
    { stdio: 'pipe' },
  );
}

// Restablece la BD dev al baseline de seeds. Lanza un error descriptivo si el
// contenedor no está disponible (mejor fallar visible que correr contra estado sucio).
export function resetToBaseline(): void {
  if (process.env.E2E_SKIP_DB_RESET === '1') {
    return;
  }
  try {
    runSql(RESET_SQL);
  } catch (error) {
    const detail = error instanceof Error ? error.message : String(error);
    throw new Error(
      `[e2e] No se pudo resetear la BD al baseline en el contenedor "${CONTAINER}". ` +
        `Asegura que SQL Server (docker) está arrancado, o exporta E2E_SKIP_DB_RESET=1 ` +
        `para omitir el reset.\nDetalle: ${detail}`,
    );
  }
}

// Crea (idempotente) el empleado dedicado con passwordMustChange=1 y devuelve sus
// credenciales de acceso inicial. Debe invocarse en el beforeEach del test, tras el
// reset del fixture.
export function seedMustChangeEmployee(): { login: string; password: string } {
  runSql(SEED_MUST_CHANGE_SQL);
  return { login: MUST_CHANGE_LOGIN, password: MUST_CHANGE_PASSWORD };
}
