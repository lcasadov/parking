import { expect, type APIRequestContext, type Page } from '@playwright/test';

// =====================================================================
// Helpers e2e (Puerta 3): login por UI + utilidades de estado vía API.
// Credenciales de los seeds de desarrollo (solo perfil des):
//   admin    → V5  (admin / Admin#Parking2026)
//   empleado → V17 (empleado / Empleado#Dev2026)
// El backend habla same-origin vía el dev-proxy de Vite (/parking-api).
// =====================================================================

export const ADMIN = { login: 'admin', password: 'Admin#Parking2026' };
export const EMPLOYEE = { login: 'empleado', password: 'Empleado#Dev2026' };

const API = '/parking-api/api/v1';

const LABELS = {
  user: 'Usuario',
  password: 'Contraseña',
  signIn: 'Entrar',
};

// Rellena y envía el formulario de login en la SPA.
export async function loginUI(page: Page, login: string, password: string): Promise<void> {
  await page.goto('/login');
  await page.getByLabel(LABELS.user).fill(login);
  await page.getByLabel(LABELS.password).fill(password);
  await page.getByRole('button', { name: LABELS.signIn }).click();
}

// Login de empleado por UI; espera aterrizar en el área de empleado.
export async function loginAsEmployee(page: Page): Promise<void> {
  await loginUI(page, EMPLOYEE.login, EMPLOYEE.password);
  await expect(page).toHaveURL(/\/employee(\/|$)/);
}

// Login de admin por UI; la landing real es /admin/employees.
export async function loginAsAdmin(page: Page): Promise<void> {
  await loginUI(page, ADMIN.login, ADMIN.password);
  await expect(page).toHaveURL(/\/admin\/employees$/);
}

// Devuelve un contexto API autenticado (cookies de sesión) para el usuario dado.
// Reutiliza el mismo baseURL (proxy Vite → backend).
export async function apiLogin(
  request: APIRequestContext,
  login: string,
  password: string,
): Promise<void> {
  const res = await request.post(`${API}/auth/login`, { data: { login, password } });
  expect(res.ok(), `login API de ${login} debe ser 200`).toBeTruthy();
}

// Cancela todas las solicitudes PENDING del empleado autenticado (idempotencia
// entre ejecuciones: la BD dev es compartida). Requiere `request` ya logueado.
export async function cancelEmployeePending(request: APIRequestContext): Promise<void> {
  const res = await request.get(`${API}/requests/mine?page=0&size=100`);
  if (!res.ok()) {
    return;
  }
  const body = (await res.json()) as { content?: Array<{ id: number; status: string }> };
  for (const req of body.content ?? []) {
    if (req.status === 'PENDING') {
      await request.post(`${API}/requests/${req.id}/cancel`);
    }
  }
}

// Fecha ISO (YYYY-MM-DD) a `offset` días de hoy, dentro de la ventana hoy..+14.
export function isoDatePlus(offset: number): string {
  const d = new Date();
  d.setDate(d.getDate() + offset);
  return d.toISOString().slice(0, 10);
}
