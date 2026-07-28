package com.aleatica.parking.systemsettings.infrastructure;

import com.aleatica.parking.systemsettings.domain.SystemSettings;
import java.math.BigDecimal;

/**
 * Mapper a mano entidad&harr;dominio del singleton {@code system-settings} (sin MapStruct).
 *
 * <p>Traduce entre la entidad de persistencia {@link SystemSettingsEntity} y el modelo de
 * dominio {@link SystemSettings} en ambos sentidos, con metodos estaticos en linea con el
 * estilo del proyecto. El mapeo es plano (todos los campos son escalares o identificadores).</p>
 */
public final class SystemSettingsMapper {

    private SystemSettingsMapper() {
        // utilidad
    }

    /**
     * Mapea la entidad de persistencia a su modelo de dominio.
     *
     * @param entity entidad origen
     * @return el modelo de dominio equivalente
     */
    public static SystemSettings toDomain(SystemSettingsEntity entity) {
        return SystemSettings.restore(
                entity.getId(), entity.getApprovalMode(), entity.getParkingAddress(),
                toDouble(entity.getParkingLat()), toDouble(entity.getParkingLng()),
                entity.isWeekendReservable(), entity.getUpdatedById(), entity.getUpdatedAt());
    }

    /**
     * Mapea el modelo de dominio a la entidad de persistencia. El {@code id} constante viaja
     * tal cual (siempre {@link SystemSettings#SINGLETON_ID}).
     *
     * @param settings modelo de dominio origen
     * @return la entidad de persistencia equivalente
     */
    public static SystemSettingsEntity toEntity(SystemSettings settings) {
        return new SystemSettingsEntity(
                (byte) settings.getId(), settings.getApprovalMode(), settings.getParkingAddress(),
                toBigDecimal(settings.getParkingLat()), toBigDecimal(settings.getParkingLng()),
                settings.isWeekendReservable(), settings.getUpdatedById(), settings.getUpdatedAt());
    }

    // La columna es DECIMAL(9,6) (BigDecimal en la entidad); el dominio y los DTOs usan
    // Double (JSON limpio para el mapa). Estos helpers puentean ambos, preservando null.
    private static Double toDouble(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    private static BigDecimal toBigDecimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }
}
