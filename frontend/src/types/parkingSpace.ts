// Tipos derivados del contrato docs/openapi.yaml (componentes ParkingSpace).

// ParkingSpace: schema #/components/schemas/ParkingSpace.
export interface ParkingSpace {
  id: number;
  label: string;
  active: boolean;
  createdAt: string;
}

// ParkingSpaceCreate: schema #/components/schemas/ParkingSpaceCreate
// (usado tambien por PUT /parking-spaces/{id}).
export interface ParkingSpaceCreate {
  label: string;
  active?: boolean;
}

// ParkingSpaceConfigureRequest: schema #/components/schemas/ParkingSpaceConfigureRequest.
export interface ParkingSpaceConfigureRequest {
  total: number;
}

// PageMeta + PageParkingSpace: schemas #/components/schemas/PageMeta y PageParkingSpace.
export interface PageParkingSpace {
  content: ParkingSpace[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

// Parametros de listado: PageParam, SizeParam y filtro active.
export interface ParkingSpaceListParams {
  page?: number;
  size?: number;
  active?: boolean;
}
