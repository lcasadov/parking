package com.aleatica.parking.export.application;

import com.aleatica.parking.audit.AuditContext;
import com.aleatica.parking.audit.AuditContextResolver;
import com.aleatica.parking.audit.AuditDetailsSerializer;
import com.aleatica.parking.audit.AuditEntry;
import com.aleatica.parking.audit.AuditRecorder;
import com.aleatica.parking.audit.AuditLog;
import com.aleatica.parking.audit.AuditLogRepository;
import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.EmployeeRepository;
import com.aleatica.parking.export.ExportTable;
import com.aleatica.parking.request.infrastructure.RequestEntity;
import com.aleatica.parking.request.infrastructure.RequestJpaRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de exportacion de datos a tabla ({@link ExportTable}), independiente del formato.
 *
 * <p>Arquitectura hexagonal: el dominio decide <em>que</em> columnas y <em>para quien</em>; el
 * formato (CSV/XLSX) lo resuelve un adaptador de serializacion en la capa web. Aplica las
 * reglas de seguridad de la capability (spec / {@code docs/security-design.md} §3, §13):</p>
 * <ul>
 *   <li>Nunca emite campos de credenciales ({@code passwordHash}, {@code failedLoginAttempts},
 *       {@code lockedUntil}): se apoya en proyecciones que ya los excluyen.</li>
 *   <li>{@code myData}/{@code myRequests} se restringen al sujeto de la sesion (BOLA).
 *       {@code myData} materializa el derecho de acceso RGPD: incluye los datos personales
 *       PROPIOS del interesado ({@code mobilePhone}, {@code licensePlate} inclusive) porque la
 *       clasificacion "Solo admins" (§13) impide que OTROS los vean, no que el sujeto reciba
 *       los suyos; nunca emite campos de credenciales.</li>
 *   <li>Cada exportacion se registra en {@code audit_log} (evento sensible / OWASP A09), con el
 *       actor resuelto del contexto y el numero de filas en {@code details}.</li>
 * </ul>
 */
@Service
public class ExportService {

    private static final String ACTION_EXPORT_EMPLOYEES = "EXPORT_EMPLOYEES";
    private static final String ACTION_EXPORT_MY_DATA = "EXPORT_MY_DATA";
    private static final String ACTION_EXPORT_REQUESTS = "EXPORT_REQUESTS";
    private static final String ACTION_EXPORT_MY_REQUESTS = "EXPORT_MY_REQUESTS";
    private static final String ACTION_EXPORT_AUDIT_LOG = "EXPORT_AUDIT_LOG";

    private static final String ENTITY_EMPLOYEE = "Employee";
    private static final String ENTITY_REQUEST = "Request";
    private static final String ENTITY_AUDIT_LOG = "AuditLog";

    private static final String BASE_EMPLOYEES = "employees";
    private static final String BASE_MY_DATA = "my-data";
    private static final String BASE_REQUESTS = "requests";
    private static final String BASE_MY_REQUESTS = "my-requests";
    private static final String BASE_AUDIT = "audit-log";

    private static final String COL_ID = "id";
    private static final String COL_EMPLOYEE_ID = "employeeId";
    private static final String COL_MSG_ACTOR_NOT_FOUND = "Usuario de sesion no encontrado: ";

    private static final List<String> EMPLOYEE_HEADERS = List.of(
            COL_ID, "firstName", "lastName", "login", "email", "department",
            "mobilePhone", "licensePlate", "isCorporate", "authOrigin", "role", "enabled", "active");
    private static final List<String> MY_DATA_HEADERS = List.of(
            COL_ID, "firstName", "lastName", "login", "email", "department",
            "mobilePhone", "licensePlate", "isCorporate", "authOrigin", "role", "enabled", "active");
    private static final List<String> REQUEST_HEADERS = List.of(
            COL_ID, COL_EMPLOYEE_ID, "requestedDate", "status", "parkingSpaceId", "approvalNote",
            "rejectionReasonCode", "rejectionReason", "resolvedById", "resolvedAt", "createdAt");
    private static final List<String> AUDIT_HEADERS = List.of(
            COL_ID, "actorEmployeeId", "action", "entityType", "entityId", "details", "occurredAt");

    private final EmployeeRepository employeeRepository;
    private final RequestJpaRepository requestRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditRecorder auditRecorder;
    private final AuditContextResolver auditContextResolver;
    private final AuditDetailsSerializer auditDetailsSerializer;

    /**
     * @param employeeRepository     repositorio de empleados (origen de empleados y datos propios)
     * @param requestRepository      repositorio de solicitudes (origen de historico y propias)
     * @param auditLogRepository     repositorio de auditoria (origen de la exportacion de auditoria)
     * @param auditRecorder          puerto de registro del evento de exportacion en {@code audit_log}
     * @param auditContextResolver   resolutor del actor (id, login) del contexto de seguridad
     * @param auditDetailsSerializer serializador del detalle enriquecido a JSON
     */
    public ExportService(
            EmployeeRepository employeeRepository,
            RequestJpaRepository requestRepository,
            AuditLogRepository auditLogRepository,
            AuditRecorder auditRecorder,
            AuditContextResolver auditContextResolver,
            AuditDetailsSerializer auditDetailsSerializer) {
        this.employeeRepository = employeeRepository;
        this.requestRepository = requestRepository;
        this.auditLogRepository = auditLogRepository;
        this.auditRecorder = auditRecorder;
        this.auditContextResolver = auditContextResolver;
        this.auditDetailsSerializer = auditDetailsSerializer;
    }

    /**
     * Exporta todos los empleados (solo {@code ADMIN}); nunca incluye campos de credenciales.
     *
     * @return tabla de empleados (posiblemente solo cabecera si no hay filas)
     */
    @Transactional(readOnly = true)
    public ExportTable exportEmployees() {
        List<List<String>> rows = employeeRepository.findAll(Sort.by(COL_ID)).stream()
                .map(ExportService::employeeRow)
                .toList();
        ExportTable table = new ExportTable(BASE_EMPLOYEES, EMPLOYEE_HEADERS, rows);
        recordAudit(ACTION_EXPORT_EMPLOYEES, ENTITY_EMPLOYEE, rows.size());
        return table;
    }

    /**
     * Exporta los datos personales del sujeto de la sesion (derecho de acceso RGPD): solo su
     * propia fila, sin campos "Solo admins" ni de credenciales.
     *
     * @param login login del usuario autenticado (sujeto de la sesion)
     * @return tabla con una unica fila (la del propio empleado)
     * @throws EntityNotFoundException si el login de la sesion no corresponde a un empleado
     */
    @Transactional(readOnly = true)
    public ExportTable exportMyData(String login) {
        Employee self = requireEmployee(login);
        List<List<String>> rows = List.of(myDataRow(self));
        ExportTable table = new ExportTable(BASE_MY_DATA, MY_DATA_HEADERS, rows);
        recordAudit(ACTION_EXPORT_MY_DATA, ENTITY_EMPLOYEE, rows.size());
        return table;
    }

    /**
     * Exporta el historico completo de solicitudes (solo {@code ADMIN}).
     *
     * @return tabla de solicitudes (posiblemente solo cabecera)
     */
    @Transactional(readOnly = true)
    public ExportTable exportRequests() {
        List<List<String>> rows = requestRepository.findAll(Sort.by(COL_ID)).stream()
                .map(ExportService::requestRow)
                .toList();
        ExportTable table = new ExportTable(BASE_REQUESTS, REQUEST_HEADERS, rows);
        recordAudit(ACTION_EXPORT_REQUESTS, ENTITY_REQUEST, rows.size());
        return table;
    }

    /**
     * Exporta las solicitudes del sujeto de la sesion (comprobacion de objeto / BOLA): nunca las
     * de otro empleado.
     *
     * @param login login del usuario autenticado (sujeto de la sesion)
     * @return tabla de solicitudes propias (posiblemente solo cabecera)
     * @throws EntityNotFoundException si el login de la sesion no corresponde a un empleado
     */
    @Transactional(readOnly = true)
    public ExportTable exportMyRequests(String login) {
        Long employeeId = requireEmployee(login).getId();
        List<List<String>> rows = requestRepository.findByEmployeeIdOrderByIdAsc(employeeId).stream()
                .map(ExportService::requestRow)
                .toList();
        ExportTable table = new ExportTable(BASE_MY_REQUESTS, REQUEST_HEADERS, rows);
        recordAudit(ACTION_EXPORT_MY_REQUESTS, ENTITY_REQUEST, rows.size());
        return table;
    }

    /**
     * Exporta el rastro de auditoria completo (solo {@code ADMIN}).
     *
     * @return tabla de entradas de auditoria (posiblemente solo cabecera)
     */
    @Transactional(readOnly = true)
    public ExportTable exportAuditLog() {
        List<List<String>> rows = auditLogRepository.findAll(Sort.by(COL_ID).descending()).stream()
                .map(ExportService::auditRow)
                .toList();
        ExportTable table = new ExportTable(BASE_AUDIT, AUDIT_HEADERS, rows);
        recordAudit(ACTION_EXPORT_AUDIT_LOG, ENTITY_AUDIT_LOG, rows.size());
        return table;
    }

    private Employee requireEmployee(String login) {
        return employeeRepository.findByLogin(login)
                .orElseThrow(() -> new EntityNotFoundException(COL_MSG_ACTOR_NOT_FOUND + login));
    }

    private void recordAudit(String action, String entityType, int rowCount) {
        AuditContext context = auditContextResolver.resolve();
        String details = auditDetailsSerializer.serialize(context, new ExportedRows(rowCount));
        auditRecorder.record(new AuditEntry(context.actorEmployeeId(), action, entityType, null, details));
    }

    private static List<String> employeeRow(Employee e) {
        List<String> row = new ArrayList<>(EMPLOYEE_HEADERS.size());
        row.add(str(e.getId()));
        row.add(e.getFirstName());
        row.add(e.getLastName());
        row.add(e.getLogin());
        row.add(e.getEmail());
        row.add(e.getDepartment());
        row.add(e.getMobilePhone());
        row.add(e.getLicensePlate());
        row.add(str(e.isCorporate()));
        row.add(str(e.getAuthOrigin()));
        row.add(str(e.getRole()));
        row.add(str(e.isEnabled()));
        row.add(str(e.isActive()));
        return row;
    }

    private static List<String> myDataRow(Employee e) {
        List<String> row = new ArrayList<>(MY_DATA_HEADERS.size());
        row.add(str(e.getId()));
        row.add(e.getFirstName());
        row.add(e.getLastName());
        row.add(e.getLogin());
        row.add(e.getEmail());
        row.add(e.getDepartment());
        // Derecho de acceso RGPD: el sujeto recibe sus PROPIOS mobilePhone/licensePlate. La
        // clasificacion "Solo admins" (§13) impide que OTROS los vean, no que el interesado
        // reciba sus datos personales (esta exportacion ya esta acotada al sujeto de la sesion).
        row.add(e.getMobilePhone());
        row.add(e.getLicensePlate());
        row.add(str(e.isCorporate()));
        row.add(str(e.getAuthOrigin()));
        row.add(str(e.getRole()));
        row.add(str(e.isEnabled()));
        row.add(str(e.isActive()));
        return row;
    }

    private static List<String> requestRow(RequestEntity r) {
        List<String> row = new ArrayList<>(REQUEST_HEADERS.size());
        row.add(str(r.getId()));
        row.add(str(r.getEmployeeId()));
        row.add(str(r.getRequestedDate()));
        row.add(str(r.getStatus()));
        row.add(str(r.getResourceId()));
        row.add(r.getApprovalNote());
        row.add(str(r.getRejectionReasonCode()));
        row.add(r.getRejectionReason());
        row.add(str(r.getResolvedById()));
        row.add(str(r.getResolvedAt()));
        row.add(str(r.getCreatedAt()));
        return row;
    }

    private static List<String> auditRow(AuditLog a) {
        List<String> row = new ArrayList<>(AUDIT_HEADERS.size());
        row.add(str(a.getId()));
        row.add(str(a.getActorEmployeeId()));
        row.add(a.getAction());
        row.add(a.getEntityType());
        row.add(str(a.getEntityId()));
        row.add(a.getDetails());
        row.add(str(a.getOccurredAt()));
        return row;
    }

    private static String str(Object value) {
        return Objects.toString(value, "");
    }

    /**
     * Detalle minimo de auditoria de una exportacion: el numero de filas emitidas. No se
     * serializa el contenido exportado en {@code audit_log} (evita duplicar datos personales
     * en el rastro).
     *
     * @param rows numero de filas de datos incluidas en el fichero
     */
    private record ExportedRows(int rows) {
    }
}
