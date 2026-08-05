// Vehículo de un visitante (change visitor-vehicles). Alias de la forma genérica compartida
// (ver types/vehicle.ts): el panel de vehículos se reutiliza entre empleados y visitantes.
export type { Vehicle as VisitorVehicle, VehicleRequest as VisitorVehicleRequest } from './vehicle';
