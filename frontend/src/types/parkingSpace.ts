// Tipos derivados del contrato docs/openapi.yaml (componentes ParkingSpace).

// ParkingSpace: schema #/components/schemas/ParkingSpace.
// `number` es la entrada (entero unico >= 1000); `label` y `floor` se derivan del
// numero en el backend (`floor = number / 1000`) y son de solo lectura.
export interface ParkingSpace {
  id: number;
  number: number;
  label: string;
  floor: number;
  active: boolean;
  createdAt: string;
}

// ParkingSpaceCreate: schema #/components/schemas/ParkingSpaceCreate
// (usado tambien por PUT /parking-spaces/{id}). Solo `number` (>= 1000) y `active`
// son campos de entrada; `label`/`floor` los deriva el backend.
export interface ParkingSpaceCreate {
  number: number;
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

// Parametros de listado: PageParam, SizeParam, filtro active y filtro floor
// (planta f = numeros f*1000..f*1000+999).
export interface ParkingSpaceListParams {
  page?: number;
  size?: number;
  active?: boolean;
  floor?: number;
}
