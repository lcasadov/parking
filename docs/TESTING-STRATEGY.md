# TESTING-STRATEGY.md — Estrategia de testing de parking

> **Autoridad de cobertura y Quality Gate.** `SONAR-STANDARDS.md` traduce las reglas Sonar "mientras se escribe"; **este documento fija los umbrales y qué se testea**. Lo consumen `tester-tdd`, `test-runner` y `verification-specialist`.
>
> **Estado del proyecto (importante):** a fecha de redacción **no existe código** (no hay `backend/`, `frontend/`, `pom.xml` ni `package.json`). Por tanto, lo que sigue es la **configuración a crear** y la **estrategia a aplicar** desde el primer commit (TDD), no "añadidos a un proyecto existente". Los nombres de clases/componentes son los **previstos** según `docs/architecture.md` (módulos hexagonales) y los mockups; se confirmarán al implementar.
>
> **Dominio:** parking (plazas/puestos ALEATICA). **Stack:** Java 21 LTS + Spring Boot 3.3 (Maven), React 18.
> **Convención:** identificadores de código y de test en inglés (ver README). Nombres de test en formato `should<Resultado>_when<Condición>()` (Given/When/Then).

---

## 1. Umbrales del Quality Gate

| Métrica | Umbral | ¿Gate cableado hoy? |
|---------|--------|---------------------|
| Cobertura de líneas | ≥ 80 % | **Sí** — JaCoCo `check` en `mvn verify` (backend); umbral Vitest en `npm run test:coverage` (frontend). Ambos corren en CI y **rompen el build**. |
| Cobertura de ramas (branches) | ≥ 75 % | **Sí** — mismo mecanismo (JaCoCo `BRANCH` / Vitest `branches`). |
| Cobertura en **flujos críticos** | 100 % | **No (aspiracional)**. JaCoCo solo verifica el ratio **global de BUNDLE** (0.80/0.75); no existe una regla por paquete/flujo que exija 100 %. Es un objetivo de diseño, no un gate automático. |
| Duplicación | ≤ 3 % | **No bloqueante en el repo hoy**. Lo mide SonarCloud, que corre **condicionado a `SONAR_TOKEN`** y **sin `sonar.qualitygate.wait=true`**: el análisis no espera al veredicto ni falla el job. |
| Tests sin aserción (S2699) | 0 | **No bloqueante en CI**. Regla de SonarCloud (misma condición que arriba); se aplica "mientras se escribe" (ver `SONAR-STANDARDS.md`), no como gate que rompa el pipeline. |
| `Thread.sleep()` en tests (S2925) | 0 | **No bloqueante en CI**. Ídem S2699. |

> **Qué gate impone qué (estado real del pipeline, `.github/workflows/ci.yml`):**
> - **Cobertura backend** → la impone `mvn -B clean verify` vía la ejecución `check` de JaCoCo a nivel **BUNDLE** (global): `LINE ≥ 0.80`, `BRANCH ≥ 0.75`. Si no se alcanza, el job `backend` falla.
> - **Cobertura frontend** → la impone `npm run test:coverage` vía los `thresholds` de Vitest (`lines 80`, `branches 75`, `functions 80`, `statements 80`). Si no se alcanza, el job `frontend` falla.
> - **SonarCloud** → corre **solo si `SONAR_TOKEN` está presente** y con `mvn sonar:sonar` **sin** `qualitygate.wait`. Por tanto, hoy **no bloquea el merge**: su Quality Gate es informativo/aspiracional, dependiente de configuración externa (proyecto en sonarcloud.io). El **frontend no se analiza con Sonar** en absoluto (no hay paso Sonar en el job `frontend`).
> - **Umbrales numéricos (0.80/0.75/80)**: coinciden con `backend/pom.xml` y `frontend/vite.config.ts`. Esos sí son gates reales.

**Flujos críticos de parking (objetivo de cobertura 100 %)** — meta de diseño, no gate automático (ver tabla). Se corrige aquí la lista heredada de plantilla (que mencionaba "AARR/AESIA/triaje", de otro proyecto):
1. **Autenticación** 🟢: verificación BCrypt, bloqueo por 5 intentos/15 min, política de contraseña, cambio obligatorio tras reset.
2. **Autorización**: RBAC `ADMIN`/`EMPLOYEE` por endpoint, y 🔵 algoritmo de `/ssocallback` (validación JWT + acceso por `login`, rol desde `Employee.role`).
3. **Disponibilidad y concurrencia**: cálculo de disponibilidad y aprobación con colisión → `409` (filtered indexes).
4. **Máquina de estados de `Request`**: transiciones válidas/ inválidas `PENDING→APPROVED/REJECTED/CANCELLED`.
5. **Auditoría**: que toda acción auditable genere su entrada en `audit_log`.

---

## 2. Backend (Java 21 + Spring Boot 3.3, Maven)

### 2.1 Configuración faltante en `pom.xml`

**Dependencias.** `spring-boot-starter-test` ya incluye JUnit 5 (Jupiter), Mockito, AssertJ y Spring Test. Solo hay que declararlo en `test` scope y añadir explícitamente `mockito-junit-jupiter` (para `@ExtendWith(MockitoExtension.class)`) y Testcontainers para integración con SQL Server:

```xml
<dependencies>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
    <!-- trae JUnit Jupiter, Mockito, AssertJ, Hamcrest, JSONassert, Spring Test -->
  </dependency>
  <dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-junit-jupiter</artifactId>
    <scope>test</scope>
  </dependency>
  <!-- Integración (slice/repos) con SQL Server real -->
  <dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
  </dependency>
  <dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mssqlserver</artifactId>
    <scope>test</scope>
  </dependency>
  <!-- Espera de estados asíncronos sin Thread.sleep (S2925) -->
  <dependency>
    <groupId>org.awaitility</groupId>
    <artifactId>awaitility</artifactId>
    <scope>test</scope>
  </dependency>
</dependencies>
```

**Surefire** (ejecuta los tests unitarios `*Test`) y **Failsafe** (integración `*IT`, opcional para separar capas):

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-surefire-plugin</artifactId>
  <!-- por defecto detecta *Test, Test*, *Tests; JUnit 5 nativo en Boot 3 -->
</plugin>
```

**JaCoCo** con verificación de umbral (falla `mvn verify` si no se alcanza):

```xml
<plugin>
  <groupId>org.jacoco</groupId>
  <artifactId>jacoco-maven-plugin</artifactId>
  <executions>
    <execution>
      <id>prepare-agent</id>
      <goals><goal>prepare-agent</goal></goals>
    </execution>
    <execution>
      <id>report</id>
      <phase>verify</phase>
      <goals><goal>report</goal></goals>
    </execution>
    <execution>
      <id>check</id>
      <phase>verify</phase>
      <goals><goal>check</goal></goals>
      <configuration>
        <rules>
          <rule>
            <element>BUNDLE</element>
            <limits>
              <limit><counter>LINE</counter><value>COVEREDRATIO</value><minimum>0.80</minimum></limit>
              <limit><counter>BRANCH</counter><value>COVEREDRATIO</value><minimum>0.75</minimum></limit>
            </limits>
          </rule>
        </rules>
        <excludes>
          <!-- no se mide cobertura sobre infraestructura sin lógica -->
          <exclude>**/ParkingApplication.*</exclude>
          <exclude>**/config/**</exclude>
          <exclude>**/dto/**</exclude>
          <exclude>**/*MapperImpl.*</exclude>
        </excludes>
      </configuration>
    </execution>
  </executions>
</plugin>
```

> Las versiones las gobierna el BOM de Spring Boot 3.3 (no fijar manualmente salvo JaCoCo y Testcontainers, que conviene anclar). SonarCloud lee el `jacoco.xml` generado en `target/site/jacoco/`.

### 2.2 Qué testear unitariamente (dominio + aplicación)

La arquitectura es **hexagonal** (`docs/architecture.md` §3): el **núcleo de dominio** se testea **sin Spring ni BD**, mockeando los **puertos de salida** con Mockito. Es donde está el 80 % del valor de los tests unitarios.

| Clase prevista (núcleo/aplicación) | Qué se prueba | Mocks (puertos de salida) | Prioridad |
|-----------------------------------|---------------|----------------------------|-----------|
| `AvailabilityPolicy` / `AvailabilityUseCase` | Las 4 condiciones de disponibilidad (activo, sin asignación fija o liberada, sin `APPROVED`, sin `VisitorReservation`) | `RepositoryPort`s | **Crítico (100 %)** |
| `RequestUseCase` (create) | Ventana 14 días; unicidad `PENDING` → conflicto; estado inicial | `RequestRepositoryPort`, `ClockPort` | **Crítico** |
| `RequestUseCase` (approve/reject/cancel) | Validar disponibilidad → 409; `rejectionReason` ≥5; transiciones de estado | repos, `MailPort`, `AuditPort` | **Crítico** |
| `RequestStatus` (máquina de estados) | Transiciones válidas e inválidas | — (lógica pura) | **Crítico** |
| `AuthService` (login local) | BCrypt OK/KO, contador de fallos, bloqueo 15 min, `passwordMustChange` | `EmployeeRepositoryPort`, `ClockPort` | **Crítico** |
| `PasswordPolicy` | ≥10, mayús/minús/dígito/símbolo, distinta de login/email | — | **Crítico** |
| `AuthorizeUseCase` (SSO) 🔵 | Validación claims, `NO_ACCESS`/`INACTIVE`, rol desde `Employee.role` (ignora claim) | `JwtPort`, `EmployeeRepositoryPort` | **Crítico** |
| `FixedAssignmentUseCase` | Unicidad plaza/día y empleado/día → 409; revocación lógica | repos | Alta |
| `ReleaseUseCase` | Voluntaria (solo dueño, fecha ≥ hoy) vs administrativa (motivo obligatorio) | repos | Alta |
| `VisitorReservationUseCase` | Ocupa plaza esa fecha; anulación solo futura | repos | Media |
| `RetentionPurgeService` | Selección de candidatos > 2 años; entidades vivas no purgadas | repos, `ClockPort` | Media |

**Patrón obligatorio: Given/When/Then + Mockito.** Ejemplo (pseudocódigo, no implementación):

```
@ExtendWith(MockitoExtension.class)
class RequestUseCaseTest {
  @Mock RequestRepositoryPort requestRepo;
  @Mock ClockPort clock;
  @InjectMocks RequestService service;   // implementación del use case

  @Test
  void shouldReturn409_whenPendingRequestAlreadyExistsForSameDate() {
    // Given
    given(clock.today()).willReturn(LocalDate.of(2026, 6, 20));
    given(requestRepo.existsPending(EMP_ID, DATE)).willReturn(true);
    // When / Then
    assertThatThrownBy(() -> service.create(EMP_ID, DATE))
        .isInstanceOf(ConflictException.class);
    then(requestRepo).should(never()).save(any());   // ≥1 aserción (S2699)
  }
}
```

Reglas (de `SONAR-STANDARDS.md` §Tests): todo test ≥1 `assert`/`expect`; **nunca `Thread.sleep()`** (usar `Awaitility`); no mockear lo que no se controla (reloj → `ClockPort` inyectable).

**Complemento por capas** (no unitario puro, para subir cobertura de adaptadores):
- **Adaptadores de entrada** (REST controllers): `@WebMvcTest` con `MockMvc` + RBAC (`@WithMockUser`) → verifican estados HTTP, validación de DTO y autorización por rol.
- **Adaptadores de salida** (repositorios JPA): `@DataJpaTest` + **Testcontainers (SQL Server)** → verifican los *filtered indexes* y las queries de disponibilidad contra una BD real (no H2: el comportamiento de los índices parciales es específico de SQL Server).

**Qué NO testear** (excluido de cobertura): DTOs, mappers generados, clases de configuración, `ParkingApplication`.

### 2.3 Comandos

```bash
mvn test       # solo unitarios (Surefire)
mvn verify     # unitarios + integración + reporte JaCoCo + check de umbral (falla si <80/75%)
```
Reporte HTML: `target/site/jacoco/index.html`. XML para SonarCloud: `target/site/jacoco/jacoco.xml`.

---

## 3. Frontend (React 18)

> El build tool no estaba fijado (`architecture.md` §16); este documento adopta **Vite + Vitest + React Testing Library**, coherente con el prompt y con el ecosistema React 18.

### 3.1 Configuración faltante

**`package.json` — devDependencies y scripts:**

```jsonc
{
  "scripts": {
    "test": "vitest run",
    "test:watch": "vitest",
    "test:coverage": "vitest run --coverage"
  },
  "devDependencies": {
    "vitest": "^2.x",
    "@vitest/coverage-v8": "^2.x",
    "jsdom": "^25.x",
    "@testing-library/react": "^16.x",
    "@testing-library/jest-dom": "^6.x",
    "@testing-library/user-event": "^14.x"
  }
}
```

**`vite.config.ts`** (la config de Vitest vive dentro del `vite.config.ts`, no en un fichero `vitest.config.ts` aparte):

```ts
import { defineConfig } from 'vite'
import { configDefaults } from 'vitest/config'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  test: {
    // Excluir los e2e de Playwright para que `npm test` no los ejecute como Vitest.
    exclude: [...configDefaults.exclude, 'e2e/**'],
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/setupTests.ts',
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html', 'lcov'],   // lcov para SonarCloud (frontend no analizado hoy)
      thresholds: { lines: 80, branches: 75, functions: 80, statements: 80 },
      exclude: ['**/*.config.*', 'src/main.tsx', 'src/**/*.d.ts', 'src/i18n/**'],
    },
  },
})
```

**`src/setupTests.ts`:**

```ts
import '@testing-library/jest-dom'
```

> **Stack de frontend fijado** (afecta a los tests): i18n **react-i18next**, estado de servidor **TanStack Query**, estado de UI **React Context**, UI **design system propio** (sin MUI). Por tanto, los componentes se renderizan en los tests con un **wrapper común** que incluye `QueryClientProvider` (con un `QueryClient` nuevo por test, sin reintentos), `I18nextProvider` y los Context de auth/tema. Extrae ese wrapper a un helper `renderWithProviders()` (evita duplicación, regla S1192/duplicación). El cliente API se mockea (no llamadas reales).

### 3.2 Qué testear (componentes y hooks)

Derivado de los mockups (`docs/mockups/`) y del diseño SPA. Componentes con **lógica/estado** (no markup estático):

| Componente / hook previsto | Qué se prueba | Prioridad |
|----------------------------|---------------|-----------|
| `LoginForm` | Validación de campos, envío, error 401, estado de carga | **Crítico** |
| `ChangePasswordForm` | Reglas de política, mensajes de error, flujo `passwordMustChange` | **Crítico** |
| `useAuth` (hook/contexto) | Sesión, `getCurrentUser`, redirección a login en 401, RBAC en UI | **Crítico** |
| `RouteGuard` / RBAC en UI | Oculta/permite rutas según `role` (`ADMIN`/`EMPLOYEE`) | **Crítico** |
| `RequestForm` (móvil) | Selección de fecha, ventana 14 días, banner de disponibilidad, envío | Alta |
| `WeeklyCalendar` (admin) | Render de estados por celda (asignada/liberada/pendiente/aprobada/libre) | Alta |
| `PendingRequestsTable` + `ApproveModal`/`RejectModal` | Aprobar (elige plaza), rechazar (motivo ≥5 obligatorio) | Alta |
| `FloorPlan` (plano) | Marcadores por coordenada/estado; click en puesto libre → solicitar | Alta |
| `MyWeek` | Render de recursos propios y huecos; **no** muestra nombres ajenos | Media |
| `useAvailability` (hook) | Fetch por fecha, estados de carga/error, cache | Media |
| `api` client (Axios `withCredentials`) | Interceptor 401 → modal sesión expirada | Media |
| `LanguageSwitcher` / `ThemeToggle` | Conmutar ES/EN y modo oscuro (estado persistente) | Baja |

**Patrón: RTL + user-event.** Render → interacción (`userEvent`) → aserción sobre DOM/efecto (`expect`), mockeando el cliente API. Cada test con ≥1 `expect` (S2699). Reglas de `SONAR-STANDARDS.md` §React: no usar índice como `key`, hooks en nivel superior, accesibilidad básica (los tests pueden seleccionar por rol/label accesible).

### 3.3 Comandos

```bash
npm run test            # vitest run (todos los tests)
npm run test:coverage   # con reporte de cobertura
```
Reporte HTML: `coverage/index.html`. `lcov.info` para SonarCloud.

---

## 4. Pirámide y tipos de test

| Nivel | Herramienta | Alcance | Peso |
|-------|-------------|---------|------|
| Unitario | JUnit5+Mockito / Vitest+RTL | Dominio, use cases, componentes/hooks | ~70 % |
| Integración (slice) | `@WebMvcTest`, `@DataJpaTest` + Testcontainers / RTL con API mock | Adaptadores, repos, controllers | ~25 % |
| E2E | **Playwright** | Journeys admin/empleado (escritorio + móvil, motor Chromium) | ~5 % |

> **E2E fuera de CI (importante).** La suite Playwright (`frontend/e2e/`) **NO está cableada en `.github/workflows/ci.yml`** — no hay job de Playwright. Es **ejecución local bajo demanda** (`npx playwright test`), porque requiere el stack real completo (SQL Server en Docker + backend Spring Boot perfil `des` + dev server Vite), que los runners no garantizan (ver `frontend/e2e/README.md`). Por tanto este ~5 % de la pirámide **no aporta cobertura en el pipeline**; los gates de CI se sostienen sobre los niveles Unitario e Integración.
>
> **Navegadores E2E (config real, `playwright.config.ts`).** Solo se definen dos proyectos, **ambos sobre motor Chromium**: `chromium` (device *Desktop Chrome*) y `mobile` (device *Pixel 5*). **No hay proyectos Firefox ni WebKit**; la suite no es multimotor.

Mocks del WS SSO 🔵 y del SMTP en los tests de integración.

---

## 5. Fechas deterministas en aserciones (evitar "time-bombs")

**Regla:** cuando el dato bajo prueba incluye timestamps generados en tiempo de ejecución (p. ej. `createdAt = hoy`, `occurredAt = ahora`), **nunca** ancles la aserción a una fecha relativa a hoy. Usa **fechas fijas de un año pasado o futuro** que jamás puedan colisionar con el timestamp del sistema.

**Por qué.** Una aserción `doesNotContain("<fecha-de-hoy>")` o `contains("<fecha-de-hoy>")` es una bomba de relojería: pasa hoy y falla otro día. **Incidente real:** el test `ExportIT` aseveraba `doesNotContain("2026-07-12")` sobre un CSV que incluye la columna `createdAt` (= hoy). El 2026-07-12 el `createdAt` de la fila coincidió con la fecha buscada y el test rompió. Se corrigió usando fechas de un año pasado fijo (`2020-03-10`, `2020-03-11`, y `doesNotContain("2020-03-12")`), que nunca coinciden con el `createdAt` de ejecución.

**Cómo aplicar:**
- Datos de negocio bajo prueba (fechas de solicitud, reservas, etc.) → **fechas literales fijas** lejanas de hoy (`LocalDate.parse("2020-03-10")`, fixtures con `'2020-01-01'`), no `LocalDate.now()±n`.
- Si la lógica **sí** depende de "hoy" (ventana 14 días, `fecha ≥ hoy`), inyecta el reloj con un `ClockPort` fijado a una fecha determinista (ver §2.2), no el reloj del sistema.
- Aserciones sobre timestamps autogenerados (`createdAt`/`occurredAt`) → afírmalo por presencia de columna/formato, no por el valor exacto del día de ejecución.

---

## 6. Pendientes / inconsistencias a reconciliar

1. **`SONAR-STANDARDS.md`** ✅ ya alineado: Java 21, design system propio (no MUI), flujos críticos de parking (§1) y nombres de test en inglés `should..._when...()`.
2. **El prompt mencionaba "reservas de pistas de pádel"**: tratado como resto de plantilla; todo se aplica al dominio **parking**.
3. **UI del frontend**: ✅ **design system propio** (CSS propio + variables + Tabler Icons, fiel a los mockups QRIA; **sin** librería de componentes tipo MUI). Build tool: **Vite** (implícito en Vitest). Los tests de componentes seleccionan por rol/label accesible.
4. **E2E**: ✅ **Playwright** (decidido). Dos proyectos sobre **motor Chromium** (`chromium` = Desktop Chrome, `mobile` = Pixel 5); **sin Firefox ni WebKit**. Auto-waits, trazas/vídeo solo en fallo. **No cableado en CI** — ejecución local bajo demanda (ver §4 y `frontend/e2e/README.md`).
5. **No existe código aún**: estas configuraciones se crean en el arranque (changes `bootstrap-mvp` / `frontend-bootstrap` del roadmap) y los nombres de clases/componentes se ajustarán a la implementación real.
