import type { UseMutationResult, UseQueryResult } from '@tanstack/react-query';

// Estado de validación / ciclo de vida de un vehículo (change employee-vehicle-self-service).
// Los vehículos de visitante no usan validación.
export type VehicleStatus = 'PENDING' | 'IN_PROGRESS' | 'APPROVED' | 'REJECTED' | 'PENDING_DELETION';

// Vehículo genérico (changes employee-vehicles / visitor-vehicles). Relación 1:N con su
// propietario (empleado o visitante); solo la matrícula es obligatoria. La UI del panel de
// vehículos no distingue el tipo de propietario: opera sobre esta forma común.
export interface Vehicle {
  id: number;
  licensePlate: string;
  brand?: string | null;
  model?: string | null;
  color?: string | null;
  // Estado de validación (self-service del empleado); opcional para el CRUD admin/visitante.
  status?: VehicleStatus;
  rejectionReason?: string | null;
  createdAt: string;
}

// Cuerpo de alta/edición de un vehículo (misma forma en ambos casos).
export interface VehicleRequest {
  licensePlate: string;
  brand?: string;
  model?: string;
  color?: string;
}

export interface VehicleUpdateVars {
  vehicleId: number;
  body: VehicleRequest;
}

// Vehículo en modo borrador (propietario aún sin id): se acumula en memoria durante el alta y
// el formulario padre lo persiste tras crear el propietario. `tempId` identifica la fila local.
export interface DraftVehicle extends VehicleRequest {
  tempId: number;
}

// Estado de borrador que el formulario padre controla y pasa al panel de vehículos.
export interface VehiclesDraft {
  vehicles: DraftVehicle[];
  onChange: (vehicles: DraftVehicle[]) => void;
}

// Conjunto de hooks react-query que el panel genérico consume. Cada propietario (empleado o
// visitante) provee su implementación (misma firma, endpoints anidados distintos), de modo que
// el panel reutiliza toda la lógica de listado/alta/edición/borrado.
export interface VehiclesHooks {
  useList: (ownerId: number | null) => UseQueryResult<Vehicle[]>;
  useCreate: (ownerId: number) => UseMutationResult<Vehicle, unknown, VehicleRequest>;
  useUpdate: (ownerId: number) => UseMutationResult<Vehicle, unknown, VehicleUpdateVars>;
  useDelete: (ownerId: number) => UseMutationResult<void, unknown, number>;
}
