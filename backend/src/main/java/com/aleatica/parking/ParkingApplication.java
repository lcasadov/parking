package com.aleatica.parking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Punto de entrada de la aplicacion parking.
 *
 * <p>Se empaqueta como WAR para desplegar sobre un Tomcat 10.1 externo (estandar
 * corporativo), por lo que extiende {@link SpringBootServletInitializer} para
 * que el contenedor de servlets arranque el contexto de Spring.</p>
 */
@SpringBootApplication
public class ParkingApplication extends SpringBootServletInitializer {

    /**
     * Configura la aplicacion cuando se despliega como WAR en un contenedor externo.
     *
     * @param builder builder proporcionado por el contenedor de servlets
     * @return el builder configurado con la clase principal de la aplicacion
     */
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(ParkingApplication.class);
    }

    /**
     * Arranque autonomo (Tomcat embebido) para desarrollo local con {@code mvn spring-boot:run}.
     *
     * @param args argumentos de linea de comandos
     */
    public static void main(String[] args) {
        SpringApplication.run(ParkingApplication.class, args);
    }
}
