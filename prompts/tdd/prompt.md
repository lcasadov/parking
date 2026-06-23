# prompt 1

Rol: Eres un Test Architect Senior con más de 15 años de experiencia diseñando estrategias de testing para aplicaciones empresariales en stacks Java/Spring Boot y React.
Contexto del proyecto: Aplicación de reservas de pistas de pádel con arquitectura hexagonal. Backend en Java 21 + Spring Boot (Maven), frontend en React 18. Lee toda la documentación disponible en el proyecto (CLAUDE.md, backlog.md, docs/) antes de responder.
Objetivo: Analiza la documentación y el código existente y devuelve un resumen estructurado con:
Backend (Java 21 + Spring Boot):

Configuración faltante en pom.xml para usar JUnit 5 + Mockito (dependencias, Surefire plugin, cobertura con JaCoCo al 80%)
Qué clases de dominio y aplicación (puertos, casos de uso, servicios) testear unitariamente para alcanzar ≥80% de cobertura, y con qué patrón (Given/When/Then + Mockito)
Comando para ejecutar: mvn test o mvn verify con reporte de cobertura

Frontend (React 18):

Configuración faltante para usar Vitest + React Testing Library (vitest.config.ts, setupTests, dependencias en package.json)
Qué componentes y hooks testear unitariamente para alcanzar ≥80% de cobertura
Comando para ejecutar: npm run test con reporte de cobertura

# prompt 2

Rol: Eres un Test Architect Senior con más de 15 años de experiencia diseñando estrategias de testing para aplicaciones empresariales en Java 21 con Spring Boot y JUnit 5.
Objetivo: Crear una suite completa de tests unitarios para backend con JUnit 5 + Mockito que valide 3 escenarios en la creación de reservas (o el recurso que aplique en cada momento):

Prueba 1: llegan todos los datos correctos y mínimos para crear el recurso
Prueba 2: llegan todos los campos rellenos (incluyendo opcionales y documentos adjuntos)
Prueba 3: llegan datos con campos obligatorios faltantes → debe lanzar excepción de validación

En las pruebas 1 y 2 se debe verificar que el repositorio (puerto de salida) recibe exactamente el objeto esperado y que el caso de uso devuelve el resultado correcto.
Genera tests unitarios para el siguiente código siguiendo estas reglas:
ESTRUCTURA

Usa @Nested para agrupar por método o escenario
Nombra cada test como: should_[acciónEsperada]_when_[condición]
Estructura cada prueba con el patrón AAA con comentarios // Arrange, // Act, // Assert

MOCKS

Usa @ExtendWith(MockitoExtension.class) en la clase de test
Usa @Mock para dependencias externas (repositorios, servicios externos)
Usa @InjectMocks para la clase bajo prueba
Usa when(...).thenReturn(...) para stubbear métodos
Usa when(...).thenThrow(...) para simular errores
Resetea mocks con @BeforeEach + Mockito.reset(...)

VERIFICACIONES

Verifica retorno con assertThat(result).isEqualTo(...) (AssertJ)
Verifica llamadas con verify(mock).metodo(argCaptor.capture())
Verifica excepciones con assertThrows(MiException.class, () -> ...)
Verifica número de llamadas con verify(mock, times(1)).metodo(...)

BASE DE DATOS

No testees el repositorio directamente en tests unitarios — mockéalo como puerto de salida
Para tests de integración con la BD usa @DataJpaTest con H2 en memoria

COBERTURA

Genera casos suficientes para alcanzar cobertura mínima del 80% (medida con JaCoCo)
Cubre siempre: happy path, excepción/error, casos borde (null, lista vacía, string vacío)

RESTRICCIONES

NO uses Object como tipo salvo que el código original lo use
NO dejes @Nested o @Test vacíos
NO mockees utilidades puras (mappers sin dependencias, etc.)
NO repitas el mismo caso con distinto nombre
NO uses Thread.sleep() — usa @MockBean del reloj si necesitas controlar el tiempo
Solo un // Act por test

OUTPUT

Un único archivo NombreClaseTest.java por cada clase testeada
Debe compilar y ejecutarse con mvn test sin errores
Incluye al inicio: imports, anotaciones de clase, @Mocks, @InjectMocks, @BeforeEach
Añade al inicio: // Cobertura estimada: 80%
Devuelve solo el código Java, sin texto adicional fuera del archivo

