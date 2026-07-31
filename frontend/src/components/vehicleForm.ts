import type { Vehicle } from '../types/vehicle';

// Estado del formulario de un vehículo (alta/edición). `id` null en alta. Compartido por el
// modal de vehículo (VehicleFormModal) y sus consumidores (CRUD admin y self-service).
export interface VehicleForm {
  id: number | null;
  licensePlate: string;
  brand: string;
  model: string;
  color: string;
}

export function emptyVehicleForm(): VehicleForm {
  return { id: null, licensePlate: '', brand: '', model: '', color: '' };
}

export function vehicleFormFrom(vehicle: Vehicle): VehicleForm {
  return {
    id: vehicle.id,
    licensePlate: vehicle.licensePlate,
    brand: vehicle.brand ?? '',
    model: vehicle.model ?? '',
    color: vehicle.color ?? '',
  };
}
