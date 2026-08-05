import { employeeVehiclesHooks } from '../hooks/useEmployeeVehicles';
import { VehiclesPanel } from './VehiclesPanel';

// Tab "Vehículos" del formulario de empleado (change employee-vehicles): reutiliza el panel
// genérico VehiclesPanel con los hooks y textos del empleado.
export function EmployeeVehiclesPanel({ employeeId }: { employeeId: number | null }) {
  return (
    <VehiclesPanel
      ownerId={employeeId}
      hooks={employeeVehiclesHooks}
      ns="employees.vehicles"
      showStatus
    />
  );
}
