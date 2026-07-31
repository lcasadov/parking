package com.aleatica.parking.visitor.application;

import com.aleatica.parking.visitor.VisitorRepository;
import com.aleatica.parking.visitor.VisitorVehicle;
import com.aleatica.parking.visitor.VisitorVehicleRepository;
import com.aleatica.parking.visitor.dto.VisitorVehicleRequest;
import com.aleatica.parking.visitor.dto.VisitorVehicleResponse;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso del CRUD de vehiculos de visitante (change {@code visitor-vehicles}); reservado al
 * {@code ADMIN} por el controlador. Acota todo por {@code visitorId} (pertenencia 1:N), normaliza
 * la matricula (trim + mayusculas) y garantiza su unicidad por visitante (409 via
 * {@link VisitorVehicleConflictException}). El borrado del visitante elimina los vehiculos por la
 * FK {@code ON DELETE CASCADE} de la BD, no aqui.
 */
@Service
public class VisitorVehicleService {

    private static final String MSG_VISITOR_NOT_FOUND = "Visitante no encontrado: ";
    private static final String MSG_VEHICLE_NOT_FOUND = "Vehiculo no encontrado: ";
    private static final String FIELD_PLATE = "licensePlate";
    private static final String MSG_PLATE_TAKEN = "El visitante ya tiene un vehiculo con esa matricula.";

    private final VisitorVehicleRepository vehicleRepository;
    private final VisitorRepository visitorRepository;

    public VisitorVehicleService(
            VisitorVehicleRepository vehicleRepository, VisitorRepository visitorRepository) {
        this.vehicleRepository = vehicleRepository;
        this.visitorRepository = visitorRepository;
    }

    @Transactional(readOnly = true)
    public List<VisitorVehicleResponse> list(Long visitorId) {
        requireVisitor(visitorId);
        return vehicleRepository.findByVisitorIdOrderByIdAsc(visitorId).stream()
                .map(VisitorVehicleResponse::from)
                .toList();
    }

    @Transactional
    public VisitorVehicleResponse create(Long visitorId, VisitorVehicleRequest request) {
        requireVisitor(visitorId);
        String plate = normalizePlate(request.licensePlate());
        if (vehicleRepository.existsByVisitorIdAndLicensePlate(visitorId, plate)) {
            throw new VisitorVehicleConflictException(FIELD_PLATE, MSG_PLATE_TAKEN);
        }
        VisitorVehicle vehicle = VisitorVehicle.create(
                visitorId, plate, trimToNull(request.brand()), trimToNull(request.model()),
                trimToNull(request.color()));
        return VisitorVehicleResponse.from(vehicleRepository.save(vehicle));
    }

    @Transactional
    public VisitorVehicleResponse update(Long visitorId, Long vehicleId, VisitorVehicleRequest request) {
        VisitorVehicle vehicle = requireVehicle(visitorId, vehicleId);
        String plate = normalizePlate(request.licensePlate());
        if (vehicleRepository.existsByVisitorIdAndLicensePlateAndIdNot(visitorId, plate, vehicleId)) {
            throw new VisitorVehicleConflictException(FIELD_PLATE, MSG_PLATE_TAKEN);
        }
        vehicle.update(
                plate, trimToNull(request.brand()), trimToNull(request.model()), trimToNull(request.color()));
        return VisitorVehicleResponse.from(vehicleRepository.save(vehicle));
    }

    @Transactional
    public void delete(Long visitorId, Long vehicleId) {
        vehicleRepository.delete(requireVehicle(visitorId, vehicleId));
    }

    private void requireVisitor(Long visitorId) {
        if (!visitorRepository.existsById(visitorId)) {
            throw new EntityNotFoundException(MSG_VISITOR_NOT_FOUND + visitorId);
        }
    }

    // Carga el vehiculo acotado al visitante: 404 si el visitante o el vehiculo no existen, o si el
    // vehiculo pertenece a otro visitante (no se filtra un vehiculo ajeno).
    private VisitorVehicle requireVehicle(Long visitorId, Long vehicleId) {
        requireVisitor(visitorId);
        return vehicleRepository.findByIdAndVisitorId(vehicleId, visitorId)
                .orElseThrow(() -> new EntityNotFoundException(MSG_VEHICLE_NOT_FOUND + vehicleId));
    }

    // Normaliza la matricula (trim + mayusculas) para comparar/persistir sin duplicados equivalentes.
    private static String normalizePlate(String plate) {
        return plate.trim().toUpperCase(Locale.ROOT);
    }

    // Convierte un opcional en blanco a null (marca/modelo/color vacios se guardan como null).
    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
