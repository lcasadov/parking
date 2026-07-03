import { test, expect, type Page } from '@playwright/test';

// =====================================================================
// Puerta 3 — e2e de login REAL (backend Spring Boot + SQL Server + SPA).
// Sin mocks: la SPA habla con el backend via dev-proxy de Vite
// (/parking-api -> localhost:8080). Credenciales del seed V5 (solo DES).
// Prerrequisitos de arranque: ver e2e/README.md.
// =====================================================================

const ADMIN_LOGIN = 'admin';
const ADMIN_PASSWORD = 'Admin#Parking2026';
const SESSION_COOKIE = 'parking_SESSION';
const AUTH_ME_PATH = '/parking-api/api/v1/auth/me';

// La UI arranca en espanol (idioma por defecto sin localStorage previo).
const LABELS = {
  user: 'Usuario',
  password: 'Contraseña',
  signIn: 'Entrar',
  logout: 'Cerrar sesión',
  invalidCredentials: 'Usuario o contraseña incorrectos',
  sessionExpiredTitle: 'Sesión expirada',
};

async function submitLogin(page: Page, login: string, password: string): Promise<void> {
  await page.goto('/login');
  await page.getByLabel(LABELS.user).fill(login);
  await page.getByLabel(LABELS.password).fill(password);
  await page.getByRole('button', { name: LABELS.signIn }).click();
}

async function loginAsAdmin(page: Page): Promise<void> {
  await submitLogin(page, ADMIN_LOGIN, ADMIN_PASSWORD);
  await expect(page).toHaveURL(/\/admin$/);
}

test('should_login_and_redirect_to_admin_when_valid_credentials', async ({ page, context }) => {
  await submitLogin(page, ADMIN_LOGIN, ADMIN_PASSWORD);

  // Redireccion al area de administracion (rol ADMIN del seed).
  await expect(page).toHaveURL(/\/admin$/);
  await expect(page.getByRole('button', { name: LABELS.logout })).toBeVisible();

  // Cookie de sesion emitida por el backend: HttpOnly (inaccesible a XSS),
  // solo observable via context.cookies() (nunca via document.cookie).
  const cookies = await context.cookies();
  const session = cookies.find((c) => c.name === SESSION_COOKIE);
  expect(session, `cookie ${SESSION_COOKIE} debe existir tras el login`).toBeDefined();
  expect(session?.httpOnly).toBe(true);
  expect(session?.value.length).toBeGreaterThan(0);
});

test('should_show_inline_error_when_invalid_credentials', async ({ page }) => {
  await submitLogin(page, ADMIN_LOGIN, 'WrongPassword#123');

  // Error inline (role=alert) y permanencia en /login.
  const alert = page.getByRole('alert');
  await expect(alert).toBeVisible();
  await expect(alert).toHaveText(LABELS.invalidCredentials);
  await expect(page).toHaveURL(/\/login$/);

  // El 401 de /auth/login NO debe disparar el modal de sesion expirada (bug #9).
  await expect(page.getByRole('dialog')).toHaveCount(0);
  await expect(page.getByText(LABELS.sessionExpiredTitle)).toHaveCount(0);
});

test('should_return_current_user_when_auth_me', async ({ page }) => {
  await loginAsAdmin(page);

  // page.request comparte las cookies del contexto del navegador:
  // el GET /auth/me viaja con la parking_SESSION real emitida en el login.
  const response = await page.request.get(AUTH_ME_PATH);
  expect(response.status()).toBe(200);

  const body = await response.json();
  expect(body.login).toBe(ADMIN_LOGIN);
  expect(body.role).toBe('ADMIN');
  expect(body.passwordMustChange).toBe(false);
});

test('should_invalidate_session_when_logout', async ({ page, context, request }) => {
  await loginAsAdmin(page);

  // Capturar el valor de la cookie ANTES del logout para probar despues
  // que el backend invalido la sesion server-side (no solo borro la cookie).
  const cookiesBefore = await context.cookies();
  const oldSession = cookiesBefore.find((c) => c.name === SESSION_COOKIE);
  expect(oldSession, 'debe existir sesion activa antes del logout').toBeDefined();

  await page.getByRole('button', { name: LABELS.logout }).click();
  await expect(page).toHaveURL(/\/login$/);

  // La cookie antigua ya no autentica: sesion invalidada en Spring Session.
  const response = await request.get(AUTH_ME_PATH, {
    headers: { Cookie: `${SESSION_COOKIE}=${oldSession?.value}` },
  });
  expect(response.status()).toBe(401);
});
