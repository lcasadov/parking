# SONAR-STANDARDS.md — Estándares de calidad de código (SonarQube)

Fuente única de las reglas de calidad de código del proyecto. La **definición y
los umbrales del Quality Gate** viven en `docs/TESTING-STRATEGY.md`; este
documento traduce las reglas Sonar a directivas que los agentes aplican
**mientras escriben**, para que el código pase el gate a la primera.

Las reglas de **seguridad** (inyección, secretos, XSS, CORS, headers, etc.) son
competencia del agente `Security` y de su checklist OWASP; aquí no se duplican.
Sonar también produce Vulnerabilities y Security Hotspots: esos hallazgos los
consume el agente `Security`, no este documento.

Regla de oro para todos los agentes: **prioriza estas reglas sobre tu intuición.
No marques una tarea como terminada hasta que el Quality Gate esté en verde.**

---

## Backend — Java 21 / Spring Boot 3.3 / Hibernate / JPA

### Spring Boot
- **Inyección por constructor, nunca por campo.** No uses `@Autowired` sobre
  campos (S6813). Dependencias como `private final` + constructor
  (`@RequiredArgsConstructor` de Lombok vale).
- **Nunca expongas entidades JPA en la capa web.** No las uses como `@RequestBody`
  ni las devuelvas desde un controlador (S4684); usa DTOs y mapea entre capas.
  (Recibir una entidad resuelta por id vía `@PathVariable` sí es aceptable.)
- Endpoints anotados con el verbo concreto (`@GetMapping`, `@PostMapping`…), no
  `@RequestMapping` genérico sin método.
- Control de errores centralizado (`@RestControllerAdvice`), no try/catch
  dispersos devolviendo 500 genéricos.
- **Nunca uses métodos `@Deprecated`** (S1874). Si un servicio tiene un método
  nuevo recomendado en el JavaDoc, úsalo directamente. Ejemplo habitual en este
  proyecto: usar el método `record(...)` de `AuditPort`/`AuditService` en lugar de
  un alias `log(...)` marcado como `@Deprecated`.
- **Constantes con nombre que contenga "PASSWORD", "SECRET", "TOKEN" o "KEY"**
  no deben tener valores con apariencia de credencial (S2068). Si la constante
  es legítima (p.ej. un hash SHA-256 de un usuario de desarrollo), añade
  `// NOSONAR: <razón>` en esa línea. Nunca hagas esto en código de producción.

### Hibernate / JPA
- Relaciones `LAZY` por defecto; nada de `FetchType.EAGER` salvo justificación.
- **Evita el N+1**: resuelve con `@EntityGraph` o `JOIN FETCH`, nunca cargando
  asociaciones dentro de un bucle.
- `equals()` / `hashCode()` de entidades sobre una *business key* estable, no
  sobre el `id` autogenerado ni todos los campos.
- `@Transactional` solo en métodos **públicos** de la capa de servicio (el proxy
  no intercepta privados ni llamadas internas same-class).
- Si una clase implementa `Serializable`, define `serialVersionUID` (S2057).

### Java general
- **Complejidad cognitiva por método < 15** (S3776). Extrae métodos, usa *early
  returns*, no anides control de flujo más de 3 niveles (S134).
- **Máximo 7 parámetros por método** (S107); si necesitas más, agrupa en un DTO.
- Nada de `System.out`/`System.err` (S106) ni `printStackTrace()` (S1148): usa
  SLF4J y loguea la excepción con contexto.
- No lances excepciones genéricas (`RuntimeException`, `Exception`, `Throwable`)
  (S112); usa específicas. Sin `catch` vacíos (S108) ni ignorados (S2486).
- Literales de cadena repetidos → constantes `static final` (S1192).
  **Aplica también dentro de arrays literales y varargs**: si una constante
  ya existe, úsala también en los array literals (p. ej. `new String[]{LABEL_ESTADO, ...}`),
  no solo en llamadas directas. Sonar cuenta todas las ocurrencias del valor, no
  solo las que no usan constante.
- `isEmpty()` en vez de `size() == 0` (S1155); compara con `equals()`, nunca con
  `==` (S4973). No anides ternarios (S3358).

### Java — Null safety y recursos
- Usa `Optional<T>` para retornos nulables en servicios; **nunca retornes `null`**
  directamente desde un método de servicio (S2583). Prefiere `.orElseThrow()` o
  `.orElse()` sobre `.get()` sin comprobación previa (S3655 — Critical Bug).
- No derreferencies un objeto sin comprobar null cuando puede ser nulo (S2259).
  Valida entradas de API con `@Valid` / `@NotNull` en vez de null-checks manuales.
- **Cierra siempre los recursos `Closeable`/`AutoCloseable` con `try-with-resources`**
  (S2095 — Critical Bug). Aplica a `InputStream`, `OutputStream`, `Connection`,
  `PreparedStatement`, etc. Nunca en un `finally` manual.
- **Prohíbe literales numéricos «mágicos»** en lógica de negocio (S109): define
  constantes `static final` con nombre descriptivo para timeouts, límites, IDs de
  estado, factores de cálculo, etc. (Excepción: `0`, `1`, `-1` en contexto obvio.)
- Métodos de clase: máximo ~30 líneas (S138); clases: máximo ~200 líneas (S2972).
  Si se supera, extrae a clase colaboradora.
- Evita cadenas de `instanceof` (S1872); usa polimorfismo o el pattern matching de
  Java 21 (`instanceof Foo f`, `switch` con patrones).

---

## Frontend — React 18 / Vite / design system propio (Vitest + Playwright)

> La UI sigue la identidad corporativa QRIA con **CSS propio + Tabler Icons** (ver mockups en `docs/mockups/`), **no** un framework de componentes tipo MUI.

### React
- Respeta las reglas de los hooks: siempre en el nivel superior, nunca dentro de
  condicionales/bucles; hooks personalizados con prefijo `use`.
- `useEffect`/`useMemo`/`useCallback` con array de dependencias **completo y
  correcto**; no lo vacíes para callar al linter.
- **No uses el índice del array como `key`**; usa un id estable.
- Un componente, una responsabilidad; extrae lógica a custom hooks; evita prop
  drilling profundo. Limpia efectos (suscripciones, timers, listeners).

### JS / TS general
- **Complejidad cognitiva por función < 15** (S3776, aplica también a JS/TS).
- `===` / `!==`, nunca `==` / `!=`. No anides ternarios (S3358).
- Nunca uses `var`; usa siempre `const` o `let` (S3504). Prohíbe `eval()` y
  `new Function()` (S4524).
- Nada de `console.log` en producción. Sin imports ni variables sin usar.
- Maneja las promesas: nada de promesas flotantes; `await` con `try/catch` o
  `.catch()`.
- En TS: evita `any` (usa `unknown` y estrecha); tipa props y respuestas de API
  (que reflejan los DTOs del backend, no las entidades).
- Accesibilidad básica: los elementos interactivos (`<button>`, `<a>`, `<input>`)
  deben tener texto accesible (`aria-label`, `title` o texto visible) (S6847).

---

## Tests — reglas de calidad (Java y TS/JS)

El código de test también lo analiza Sonar. Las siguientes reglas aplican a
`src/test/**` (Java) y `**/*.test.{ts,tsx,js,jsx}` / `**/*.spec.*` (Frontend).

- **Todo test debe tener al menos un `assert` / `expect`** (S2699 — Bug). Un
  test sin aserciones pasa siempre en verde aunque el código esté roto.
- **Prohibido `Thread.sleep()` en tests backend** (S2925). Usa `Awaitility`,
  `CompletableFuture` o mecanismos reactivos para esperar estados asíncronos.
- **Cobertura mínima obligatoria** (definida en `docs/TESTING-STRATEGY.md`):
  ≥ 80 % de líneas · ≥ 75 % de branches · **100 %** en los flujos críticos de
  parking (autenticación, autorización/SSO, disponibilidad y concurrencia de
  aprobación, máquina de estados de `Request`, auditoría). El CI bloquea el merge
  si no se cumple.
- Los métodos de test Java deben estar anotados con `@Test` o `@ParameterizedTest`
  y con nombre descriptivo en formato `should<Resultado>_when<Condición>()`
  (Given/When/Then), en inglés, según la convención de nomenclatura del proyecto.
- No mockees lo que no controlas (servicios externos, el reloj del sistema); usa
  abstracciones inyectables.

---

## Duplicación de código

- **Umbral máximo de duplicación: 3 %** del total del proyecto. Sonar bloquea el
  Quality Gate si se supera.
- No copies bloques de > 10 líneas: extrae método, clase utilitaria o constante
  compartida.
- En tests: está permitido cierto boilerplate de setup, pero si un bloque
  idéntico aparece en ≥ 3 tests, extrae un método `@BeforeEach` / `beforeEach`.

---

## Proceso ante hallazgos de Sonar

### ⚠️ Trampa habitual: extraer métodos privados introduce violations en código preexistente

Cuando reduces la complejidad (S3776) extrayendo un método privado, **Sonar
considera que todas las líneas del nuevo método son "nuevas"** en el diff de la PR.
Si esas líneas contenían literals duplicados, API deprecated, etc., Sonar los
contará como `new_violations` aunque el código existiera antes.

**Regla**: al extraer un método privado, revisa las reglas S1192, S1874 y S2068
en las líneas extraídas antes de hacer el commit. Aplica las constantes y
sustituciones en el mismo commit de la extracción.

### Corrección normal
Corrige el issue antes de abrir el PR. Si el Quality Gate está en rojo, el merge
está bloqueado.

### Falso positivo o supresión justificada
- **Java**: `// NOSONAR: <razón concreta>` en la línea afectada. Sin razón, el
  revisor lo revertirá.
  ```java
  catch (InterruptedException e) { // NOSONAR: relanzamos como RuntimeException inmediatamente
      Thread.currentThread().interrupt();
      throw new ProcessingException(e);
  }
  ```
- **JS/TS**: `// NOSONAR` al final de la línea, con comentario de bloque encima.
  ```ts
  // NOSONAR: función externa legada sin tipado; no modificable en este sprint
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const data = legacyLib.parse(raw) as any; // NOSONAR
  ```
- **`// NOSONAR` debe estar en la misma línea que el código que suprime**, nunca
  en la línea anterior. Sonar lo ignora si está en una línea separada:
  ```java
  // ❌ INCORRECTO — Sonar ignora esto
  // NOSONAR: razón
  private static final String SECRET = "valor";

  // ✅ CORRECTO — Sonar suprime la regla en esta línea
  private static final String SECRET = "valor"; // NOSONAR: razón concreta
  ```
- **Nunca suprimas reglas de categoría Vulnerability o Security Hotspot.**
  Esas las gestiona el agente `Security` mediante revisión explícita en Sonar UI.
