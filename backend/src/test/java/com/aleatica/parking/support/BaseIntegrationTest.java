package com.aleatica.parking.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MSSQLServerContainer;

/**
 * Clase base de los tests de integracion: arranca un SQL Server 2022 real en
 * Testcontainers (no H2), para verificar el comportamiento real de Flyway, la
 * sesion JDBC y los indices de SQL Server.
 *
 * <p>El contenedor es un <strong>singleton</strong> compartido por todas las
 * subclases: se arranca una sola vez y nunca se detiene explicitamente (lo limpia
 * el ryuk de Testcontainers o el apagado de la JVM). Asi la cache de contextos de
 * Spring siempre apunta a un contenedor vivo, evitando que un contexto reutilizado
 * referencie un contenedor ya detenido.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public abstract class BaseIntegrationTest {

    private static final String MSSQL_IMAGE =
            "mcr.microsoft.com/mssql/server:2022-latest";

    /** Contenedor SQL Server compartido (singleton) entre todos los tests de integracion. */
    @SuppressWarnings("resource") // ciclo de vida gestionado por el singleton + JVM shutdown
    protected static final MSSQLServerContainer<?> SQL_SERVER;

    static {
        SQL_SERVER = new MSSQLServerContainer<>(MSSQL_IMAGE).acceptLicense();
        SQL_SERVER.start();
    }

    /**
     * Login del seed admin de desarrollo (Flyway V5). Se preserva en cada limpieza:
     * es la unica fila que sobrevive al reset de estado entre tests.
     */
    protected static final String SEED_ADMIN_LOGIN = "admin";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    private JdbcTemplate baseJdbcTemplate;

    /**
     * Aisla cada test del estado dejado por los demas en el contenedor SQL Server
     * compartido (singleton). Se ejecuta ANTES del {@code @BeforeEach} de la subclase
     * (garantia de orden de JUnit 5: superclase primero), de modo que cada IT arranca
     * desde una BD limpia y ningun IT contamina a otro (issue #29).
     *
     * <p>El borrado respeta el orden FK-safe (hijos primero): {@code visitor_reservations}
     * referencia {@code visitors}, {@code parking_spaces} y {@code employees};
     * {@code releases}, {@code requests} y {@code fixed_assignments} referencian
     * {@code parking_spaces} y {@code employees}; {@code visitors} referencia
     * {@code employees}; {@code audit_log} y {@code login_log} referencian
     * {@code employees}. Por eso {@code visitor_reservations} se borra ANTES que
     * {@code visitors}/{@code parking_spaces}, y {@code releases}/{@code requests}/
     * {@code visitors} ANTES que {@code parking_spaces}/{@code employees}, para que ningun
     * IT filtre filas a otro (issue #29). Se conserva el historico de Flyway y el seed
     * admin (V5); a este ultimo se le resetea el estado de bloqueo por si un test previo
     * acumulo intentos fallidos.</p>
     */
    @BeforeEach
    void resetDomainState() {
        baseJdbcTemplate.update("DELETE FROM dbo.visitor_reservations");
        baseJdbcTemplate.update("DELETE FROM dbo.releases");
        baseJdbcTemplate.update("DELETE FROM dbo.requests");
        baseJdbcTemplate.update("DELETE FROM dbo.fixed_assignments");
        baseJdbcTemplate.update("DELETE FROM dbo.audit_log");
        baseJdbcTemplate.update("DELETE FROM dbo.login_log");
        baseJdbcTemplate.update("DELETE FROM dbo.visitors");
        baseJdbcTemplate.update("DELETE FROM dbo.parking_spaces");
        baseJdbcTemplate.update("DELETE FROM dbo.employees WHERE login <> ?", SEED_ADMIN_LOGIN);
        baseJdbcTemplate.update(
                "UPDATE dbo.employees SET failed_login_attempts = 0, locked_until = NULL, "
                        + "active = 1 WHERE login = ?",
                SEED_ADMIN_LOGIN);
    }

    /**
     * Inyecta la URL y credenciales del contenedor en el contexto de Spring.
     *
     * <p>La URL fija {@code databaseName=master} (la base por defecto del contenedor)
     * y {@code encrypt=true;trustServerCertificate=true} para cifrar confiando en el
     * certificado autofirmado del contenedor efimero.</p>
     *
     * @param registry registro de propiedades dinamicas de Spring Test
     */
    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", BaseIntegrationTest::jdbcUrl);
        registry.add("spring.datasource.username", SQL_SERVER::getUsername);
        registry.add("spring.datasource.password", SQL_SERVER::getPassword);
        registry.add("spring.datasource.driver-class-name",
                () -> "com.microsoft.sqlserver.jdbc.SQLServerDriver");
    }

    private static String jdbcUrl() {
        return "jdbc:sqlserver://%s:%d;databaseName=master;encrypt=true;trustServerCertificate=true"
                .formatted(SQL_SERVER.getHost(), SQL_SERVER.getFirstMappedPort());
    }
}
