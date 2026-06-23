package com.aleatica.parking.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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

    @Autowired
    protected MockMvc mockMvc;

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
