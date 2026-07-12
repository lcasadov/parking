import { execFileSync } from 'node:child_process';

// =====================================================================
// Reset de aislamiento (Puerta 3): vacia la tabla `requests` de la BD dev.
//
// Los tests usan `cancelEmployeePending` para idempotencia, pero cancelar NO
// borra, y `admin-approval` ademas deja solicitudes APPROVED/REJECTED que el
// guard de duplicados bloquea al recrearlas. Sin un reset, el estado se acumula
// run tras run y rompe las asserts paginadas por fecha y el propio setup.
//
// Se invoca ANTES DE CADA test (fixture auto-use en `fixtures.ts`): resetear una
// sola vez (globalSetup) no basta en modo --ui, donde re-lanzar un test no
// re-ejecuta el setup global.
//
// Ningun seed de Flyway inserta requests y ninguna FK apunta a la tabla, asi que
// el DELETE es seguro. Configurable por entorno para no acoplarlo a esta maquina:
//   E2E_DB_CONTAINER  contenedor docker de SQL Server (def. parking-sqlserver)
//   MSSQL_SA_PASSWORD contrasena de sa               (def. Parking!Local2024)
//   E2E_DB_NAME       base de datos                  (def. parking)
//   E2E_SKIP_DB_RESET '1' para omitir el reset (p. ej. si no hay docker)
// =====================================================================

const CONTAINER = process.env.E2E_DB_CONTAINER ?? 'parking-sqlserver';
const SA_PASSWORD = process.env.MSSQL_SA_PASSWORD ?? 'Parking!Local2024';
const DB_NAME = process.env.E2E_DB_NAME ?? 'parking';
const SQLCMD = '/opt/mssql-tools18/bin/sqlcmd';

// QUOTED_IDENTIFIER ON es obligatorio: `requests` tiene un indice filtrado.
const RESET_SQL = 'SET QUOTED_IDENTIFIER ON; SET NOCOUNT ON; DELETE FROM requests;';

// Vacia la tabla `requests` para partir de un baseline limpio. Lanza un error
// descriptivo si el contenedor no esta disponible (mejor fallar visible que
// correr contra estado sucio).
export function resetRequests(): void {
  if (process.env.E2E_SKIP_DB_RESET === '1') {
    return;
  }
  try {
    execFileSync(
      'docker',
      [
        'exec',
        CONTAINER,
        SQLCMD,
        '-S',
        'localhost',
        '-U',
        'sa',
        '-P',
        SA_PASSWORD,
        '-C',
        '-b',
        '-d',
        DB_NAME,
        '-Q',
        RESET_SQL,
      ],
      { stdio: 'pipe' },
    );
  } catch (error) {
    const detail = error instanceof Error ? error.message : String(error);
    throw new Error(
      `[e2e] No se pudo resetear la tabla \`requests\` en el contenedor ` +
        `"${CONTAINER}". Asegura que SQL Server (docker) esta arrancado, o exporta ` +
        `E2E_SKIP_DB_RESET=1 para omitir el reset.\nDetalle: ${detail}`,
    );
  }
}
