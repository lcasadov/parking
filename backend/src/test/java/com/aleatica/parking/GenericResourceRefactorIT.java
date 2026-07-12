package com.aleatica.parking;

import static org.assertj.core.api.Assertions.assertThat;

import com.aleatica.parking.fixedassignment.FixedAssignment;
import com.aleatica.parking.fixedassignment.FixedAssignmentRepository;
import com.aleatica.parking.availability.application.AvailabilityService;
import com.aleatica.parking.availability.dto.AvailabilityItemResponse;
import com.aleatica.parking.release.Release;
import com.aleatica.parking.release.ReleaseRepository;
import com.aleatica.parking.request.infrastructure.RequestEntity;
import com.aleatica.parking.request.infrastructure.RequestJpaRepository;
import com.aleatica.parking.parkingspace.ParkingSpaceResourceResolver;
import com.aleatica.parking.resource.BookableResource;
import com.aleatica.parking.resource.ResourceType;
import com.aleatica.parking.support.BaseIntegrationTest;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Tests de integracion del refactor a recurso generico (change
 * {@code init-generic-resource-refactor}) contra un SQL Server real.
 *
 * <p>Verifica el modelo generalizado ({@code resource_id} + {@code resource_type}) sobre
 * {@code fixed_assignments}, {@code requests} y {@code releases} SIN cambiar el
 * comportamiento observable para {@code PARKING}: las filas se modelan como recurso
 * {@code PARKING}, la migracion porta los datos a {@code resource_id} con
 * {@code resource_type = 'PARKING'}, la disponibilidad devuelve el mismo conjunto de
 * recursos y ninguna consulta del nucleo devuelve recursos {@code DESK}.</p>
 */
class GenericResourceRefactorIT extends BaseIntegrationTest {

    private static final LocalDate DATE = LocalDate.of(2026, 7, 10); // viernes -> dow 5
    private static final int DATE_DOW = DATE.getDayOfWeek().getValue();

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private FixedAssignmentRepository fixedAssignmentRepository;

    @Autowired
    private RequestJpaRepository requestRepository;

    @Autowired
    private ReleaseRepository releaseRepository;

    @Autowired
    private AvailabilityService availabilityService;

    // Adaptador PARKING concreto: tras la capability desks hay dos ResourceResolverPort
    // (PARKING/DESK), asi que este IT del nucleo de parking inyecta el de plaza explicitamente.
    @Autowired
    private ParkingSpaceResourceResolver resourceResolver;

    // ---- Req 1: modelo generalizado ----

    @Test
    void should_model_fixed_assignment_as_PARKING_resource_when_assignment_is_persisted() {
        // Arrange
        long employeeId = insertEmployee("emp-fa");
        long spaceId = insertSpace("P-FA");

        // Act
        FixedAssignment saved = fixedAssignmentRepository.saveAndFlush(
                FixedAssignment.create(spaceId, employeeId, DATE_DOW, seedAdminId(), Instant.now()));

        // Assert: recurso PARKING con resource_id = plaza; la columna persistida es PARKING
        assertThat(saved.getResourceId()).isEqualTo(spaceId);
        assertThat(saved.getResourceType()).isEqualTo(ResourceType.PARKING);
        assertThat(columnResourceType("fixed_assignments", saved.getId())).isEqualTo("PARKING");
        // La resolucion de BookableResource devuelve la misma plaza (mismo id, tipo PARKING)
        Optional<BookableResource> resolved = resourceResolver.resolve(saved.getResourceId());
        assertThat(resolved).isPresent();
        assertThat(resolved.orElseThrow().getResourceId()).isEqualTo(spaceId);
        assertThat(resolved.orElseThrow().getResourceType()).isEqualTo(ResourceType.PARKING);
    }

    @Test
    void should_keep_resource_id_null_with_type_PARKING_when_request_is_pending() {
        // Arrange
        long employeeId = insertEmployee("emp-pending");

        // Act
        RequestEntity saved = requestRepository.saveAndFlush(RequestEntity.create(employeeId, DATE, Instant.now()));

        // Assert: PENDING sin recurso asignado, pero tipo PARKING por defecto del nucleo
        assertThat(saved.getResourceId()).isNull();
        assertThat(saved.getResourceType()).isEqualTo(ResourceType.PARKING);
        Long dbResourceId = jdbcTemplate.queryForObject(
                "SELECT resource_id FROM dbo.requests WHERE id = ?", Long.class, saved.getId());
        assertThat(dbResourceId).isNull();
        assertThat(columnResourceType("requests", saved.getId())).isEqualTo("PARKING");
    }

    // ---- Req 2: no-regresion de disponibilidad ----

    @Test
    void should_return_same_available_resources_when_calculating_availability_after_refactor() {
        // Arrange: A ocupada por asignacion fija; B asignada pero liberada esa fecha; C libre.
        // Dos titulares distintos (la unicidad empleado/dia impide dos asignaciones del mismo
        // empleado el mismo dia).
        long employeeOccupied = insertEmployee("emp-avail-occ");
        long employeeReleased = insertEmployee("emp-avail-rel");
        long spaceOccupied = insertSpace("P-OCC");
        long spaceReleased = insertSpace("P-REL");
        long spaceFree = insertSpace("P-FREE");
        fixedAssignmentRepository.saveAndFlush(
                FixedAssignment.create(spaceOccupied, employeeOccupied, DATE_DOW, seedAdminId(), Instant.now()));
        fixedAssignmentRepository.saveAndFlush(
                FixedAssignment.create(spaceReleased, employeeReleased, DATE_DOW, seedAdminId(), Instant.now()));
        releaseRepository.saveAndFlush(
                Release.voluntary(spaceReleased, employeeReleased, DATE, Instant.now()));

        // Act
        List<Long> available = availabilityService.availabilityForDate(DATE).availableResources().stream()
                .map(AvailabilityItemResponse::parkingSpaceId)
                .toList();

        // Assert: la plaza liberada y la libre estan disponibles; la ocupada no (regla identica)
        assertThat(available).contains(spaceReleased, spaceFree);
        assertThat(available).doesNotContain(spaceOccupied);
    }

    // ---- Req 3: migracion de datos ----

    @Test
    void should_port_existing_references_to_resource_id_with_type_PARKING_when_migration_runs() {
        // Arrange: se inserta por SQL crudo SIN indicar resource_type (como haria el backfill de
        // la migracion sobre datos existentes); el DEFAULT debe fijarlo a 'PARKING'.
        long employeeId = insertEmployee("emp-port");
        long spaceId = insertSpace("P-PORT");
        jdbcTemplate.update(
                "INSERT INTO dbo.fixed_assignments (resource_id, employee_id, day_of_week, "
                        + "active, created_by_id, created_at) VALUES (?, ?, ?, 1, ?, SYSUTCDATETIME())",
                spaceId, employeeId, DATE_DOW, seedAdminId());
        jdbcTemplate.update(
                "INSERT INTO dbo.releases (resource_id, employee_id, release_date, type, "
                        + "released_by_id, created_at) VALUES (?, ?, ?, 'VOLUNTARY', ?, SYSUTCDATETIME())",
                spaceId, employeeId, Date.valueOf(DATE), employeeId);

        // Assert: cada fila conserva resource_id = plaza original y resource_type = 'PARKING'
        assertThat(count("SELECT COUNT(*) FROM dbo.fixed_assignments "
                + "WHERE resource_id = ? AND resource_type = 'PARKING'", spaceId)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM dbo.releases "
                + "WHERE resource_id = ? AND resource_type = 'PARKING'", spaceId)).isEqualTo(1);
        // Ninguna fila queda con resource_type nulo tras el backfill.
        assertThat(count("SELECT COUNT(*) FROM dbo.fixed_assignments WHERE resource_type IS NULL"))
                .isZero();
    }

    // ---- Casos limite: enum admite DESK pero el nucleo no tiene filas DESK ----

    @Test
    void should_not_return_DESK_resources_when_querying_parking_core() {
        // Arrange: hay actividad PARKING en el nucleo
        long employeeId = insertEmployee("emp-desk");
        long spaceId = insertSpace("P-DESK");
        fixedAssignmentRepository.saveAndFlush(
                FixedAssignment.create(spaceId, employeeId, DATE_DOW, seedAdminId(), Instant.now()));

        // Assert: el enum admite DESK, pero ninguna tabla del nucleo tiene filas DESK
        assertThat(ResourceType.values()).contains(ResourceType.DESK);
        assertThat(count("SELECT COUNT(*) FROM dbo.fixed_assignments WHERE resource_type = 'DESK'"))
                .isZero();
        assertThat(count("SELECT COUNT(*) FROM dbo.requests WHERE resource_type = 'DESK'")).isZero();
        assertThat(count("SELECT COUNT(*) FROM dbo.releases WHERE resource_type = 'DESK'")).isZero();
    }

    // ---- Helpers ----

    private String columnResourceType(String table, long id) {
        return jdbcTemplate.queryForObject(
                "SELECT resource_type FROM dbo." + table + " WHERE id = ?", String.class, id);
    }

    private long insertEmployee(String login) {
        jdbcTemplate.update(
                "INSERT INTO dbo.employees "
                        + "(first_name, last_name, login, email, password_hash, password_must_change, "
                        + "is_corporate, auth_origin, role, enabled, active) "
                        + "VALUES ('IT', 'User', ?, ?, 'x', 0, 0, 'LOCAL', 'EMPLOYEE', 1, 1)",
                login, login + "@aleatica.com");
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM dbo.employees WHERE login = ?", Long.class, login);
        return id == null ? 0L : id;
    }

    private long insertSpace(String label) {
        jdbcTemplate.update("INSERT INTO dbo.parking_spaces (label, active) VALUES (?, 1)", label);
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM dbo.parking_spaces WHERE label = ?", Long.class, label);
        return id == null ? 0L : id;
    }

    private long seedAdminId() {
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM dbo.employees WHERE login = ?", Long.class, SEED_ADMIN_LOGIN);
        return id == null ? 0L : id;
    }

    private int count(String sql, Object... args) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return value == null ? 0 : value;
    }
}
