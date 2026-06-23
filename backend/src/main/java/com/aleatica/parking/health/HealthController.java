package com.aleatica.parking.health;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint publico de comprobacion de vida (liveness) de la API.
 *
 * <p>Separado del Actuator para ofrecer un contrato estable y minimo
 * ({@code {"status":"UP"}}) sin exponer detalles de infraestructura.</p>
 */
@Tag(name = "Health", description = "Comprobacion de disponibilidad de la API")
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    private static final String STATUS_KEY = "status";
    private static final String STATUS_UP = "UP";

    /**
     * Indica que la API esta operativa.
     *
     * @return mapa {@code {"status":"UP"}} con estado HTTP 200
     */
    @Operation(summary = "Estado de la API", description = "Devuelve UP si la API responde.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "La API esta operativa"))
    @GetMapping
    public Map<String, String> health() {
        return Map.of(STATUS_KEY, STATUS_UP);
    }
}
