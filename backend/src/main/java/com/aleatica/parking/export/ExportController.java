package com.aleatica.parking.export;

import com.aleatica.parking.auth.domain.ClockPort;
import com.aleatica.parking.exception.ApiError;
import com.aleatica.parking.export.application.ExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de exportacion a CSV/XLSX (capability {@code exports}), transversal a varias
 * pantallas. Consolida en un unico pipeline las cinco exportaciones del contrato
 * ({@code docs/openapi.yaml}, tag {@code Exports}).
 *
 * <p>El adaptador web no contiene logica de negocio: valida el formato, aplica el limite de
 * tasa, delega la construccion de la tabla en {@link ExportService} (que ademas registra el
 * evento en {@code audit_log}) y serializa en streaming sobre el {@code OutputStream} de la
 * respuesta mediante el {@link ExportWriterPort} resuelto por {@link ExportWriters}. La
 * autorizacion es de dos niveles: RBAC por endpoint ({@code @PreAuthorize}) y, en las
 * exportaciones "propias", comprobacion de objeto (BOLA) por el sujeto de la sesion, que el
 * servicio garantiza filtrando por el {@code login} autenticado. El orden de comprobaciones es
 * fail-fast y fail-closed: primero formato ({@code 400}), luego limite de tasa ({@code 429}) y
 * solo entonces se consultan datos y se audita, de modo que una peticion rechazada no genera
 * fichero ni registro.</p>
 */
@Tag(name = "Exports", description = "Exportaciones a CSV/XLSX")
@RestController
@RequestMapping("/api/v1")
public class ExportController {

    private static final String SESSION_COOKIE = "sessionCookie";
    private static final String FORMAT_PARAM = "format";
    private static final String FORMAT_DESC = "Formato de exportacion (csv o xlsx; por defecto xlsx)";
    private static final DateTimeFormatter FILE_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC);

    private final ExportService exportService;
    private final ExportWriters exportWriters;
    private final ExportRateLimiter rateLimiter;
    private final ClockPort clock;

    /**
     * @param exportService casos de uso de exportacion (construccion de tablas + auditoria)
     * @param exportWriters resolutor de adaptadores de serializacion por formato
     * @param rateLimiter   limitador de tasa de exportaciones (5/min por usuario)
     * @param clock         reloj inyectable (UTC) para el timestamp del nombre de fichero
     */
    public ExportController(
            ExportService exportService,
            ExportWriters exportWriters,
            ExportRateLimiter rateLimiter,
            ClockPort clock) {
        this.exportService = exportService;
        this.exportWriters = exportWriters;
        this.rateLimiter = rateLimiter;
        this.clock = clock;
    }

    /**
     * Exporta todos los empleados (solo {@code ADMIN}); el fichero nunca incluye campos de
     * credenciales.
     *
     * @param format         formato solicitado (defecto {@code xlsx})
     * @param authentication autenticacion resuelta de la sesion
     * @return {@code 200} con el fichero adjunto
     */
    @Operation(summary = "Exporta empleados a CSV/XLSX (ADMIN)",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fichero exportado"),
            @ApiResponse(responseCode = "400", description = "Formato no soportado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "429", description = "Limite de exportaciones superado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/employees/export")
    @PreAuthorize("hasRole('ADMIN')")
    public void exportEmployees(
            @Parameter(description = FORMAT_DESC)
            @RequestParam(name = FORMAT_PARAM, required = false) String format,
            Authentication authentication, HttpServletResponse response) throws IOException {
        respond(authentication.getName(), format, exportService::exportEmployees, response);
    }

    /**
     * Exporta los datos personales del usuario autenticado (derecho de acceso RGPD): solo los
     * suyos, sin campos "Solo admins".
     *
     * @param format         formato solicitado (defecto {@code xlsx})
     * @param authentication autenticacion resuelta de la sesion (sujeto)
     * @return {@code 200} con el fichero adjunto
     */
    @Operation(summary = "Exporta mis propios datos personales (EMPLOYEE/ADMIN)",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fichero exportado"),
            @ApiResponse(responseCode = "400", description = "Formato no soportado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "429", description = "Limite de exportaciones superado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/employees/me/export")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public void exportMyData(
            @Parameter(description = FORMAT_DESC)
            @RequestParam(name = FORMAT_PARAM, required = false) String format,
            Authentication authentication, HttpServletResponse response) throws IOException {
        String login = authentication.getName();
        respond(login, format, () -> exportService.exportMyData(login), response);
    }

    /**
     * Exporta el historico completo de solicitudes (solo {@code ADMIN}).
     *
     * @param format         formato solicitado (defecto {@code xlsx})
     * @param authentication autenticacion resuelta de la sesion
     * @return {@code 200} con el fichero adjunto
     */
    @Operation(summary = "Exporta el historico de solicitudes (ADMIN)",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fichero exportado"),
            @ApiResponse(responseCode = "400", description = "Formato no soportado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "429", description = "Limite de exportaciones superado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/requests/export")
    @PreAuthorize("hasRole('ADMIN')")
    public void exportRequests(
            @Parameter(description = FORMAT_DESC)
            @RequestParam(name = FORMAT_PARAM, required = false) String format,
            Authentication authentication, HttpServletResponse response) throws IOException {
        respond(authentication.getName(), format, exportService::exportRequests, response);
    }

    /**
     * Exporta las solicitudes propias del usuario autenticado (comprobacion de objeto / BOLA).
     *
     * @param format         formato solicitado (defecto {@code xlsx})
     * @param authentication autenticacion resuelta de la sesion (sujeto)
     * @return {@code 200} con el fichero adjunto
     */
    @Operation(summary = "Exporta mis solicitudes a CSV/XLSX (EMPLOYEE/ADMIN)",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fichero exportado"),
            @ApiResponse(responseCode = "400", description = "Formato no soportado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "429", description = "Limite de exportaciones superado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/requests/mine/export")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public void exportMyRequests(
            @Parameter(description = FORMAT_DESC)
            @RequestParam(name = FORMAT_PARAM, required = false) String format,
            Authentication authentication, HttpServletResponse response) throws IOException {
        String login = authentication.getName();
        respond(login, format, () -> exportService.exportMyRequests(login), response);
    }

    /**
     * Exporta el rastro de auditoria (solo {@code ADMIN}).
     *
     * @param format         formato solicitado (defecto {@code xlsx})
     * @param authentication autenticacion resuelta de la sesion
     * @return {@code 200} con el fichero adjunto
     */
    @Operation(summary = "Exporta la auditoria a CSV/XLSX (ADMIN)",
            security = @SecurityRequirement(name = SESSION_COOKIE))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fichero exportado"),
            @ApiResponse(responseCode = "400", description = "Formato no soportado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "429", description = "Limite de exportaciones superado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/audit/export")
    @PreAuthorize("hasRole('ADMIN')")
    public void exportAuditLog(
            @Parameter(description = FORMAT_DESC)
            @RequestParam(name = FORMAT_PARAM, required = false) String format,
            Authentication authentication, HttpServletResponse response) throws IOException {
        respond(authentication.getName(), format, exportService::exportAuditLog, response);
    }

    /**
     * Pipeline comun de exportacion: valida formato ({@code 400}), aplica limite de tasa
     * ({@code 429}), construye la tabla (consulta + auditoria) y devuelve el fichero en streaming.
     *
     * @param userKey       clave de usuario para el limite de tasa (login de la sesion)
     * @param formatParam   valor del parametro {@code format}
     * @param tableSupplier proveedor perezoso de la tabla (solo se invoca tras superar los controles)
     * @param response      respuesta HTTP sobre cuyo {@code OutputStream} se serializa el fichero
     * @throws IOException si falla la escritura sobre el flujo de salida
     */
    private void respond(String userKey, String formatParam,
            Supplier<ExportTable> tableSupplier, HttpServletResponse response) throws IOException {
        ExportFormat exportFormat = ExportFormat.fromParam(formatParam);
        rateLimiter.acquire(userKey);
        ExportWriterPort writer = exportWriters.forFormat(exportFormat);
        ExportTable table = tableSupplier.get();
        String filename = table.baseName() + "-" + FILE_TIMESTAMP.format(clock.now())
                + "." + writer.extension();
        response.setContentType(writer.contentType());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + filename + "\"");
        writer.write(table, response.getOutputStream());
    }
}
