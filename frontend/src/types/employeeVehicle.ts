// Vehículo de un empleado (change employee-vehicles). Alias de la forma genérica compartida
// (ver types/vehicle.ts): el panel de vehículos se reutiliza entre empleados y visitantes.
export type { Vehicle as EmployeeVehicle, VehicleRequest as EmployeeVehicleRequest } from './vehicle';
