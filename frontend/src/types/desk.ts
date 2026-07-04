// Tipos derivados del contrato docs/openapi.yaml (componentes Desk).
// NOTA DE CONTRATO: los endpoints /desks los añade el backend en este mismo change
// (init-desks §2.5). Estos tipos reflejan el contrato asumido, espejo de ParkingSpace:
// Desk { id, number(1-65), category, coordX, coordY(0-100), active, createdAt }.

// DeskCategory: schema #/components/schemas/DeskCategory.
export type DeskCategory = 'STANDARD' | 'EXECUTIVE';

// Desk: schema #/components/schemas/Desk.
export interface Desk {
  id: number;
  number: number;
  category: DeskCategory;
  coordX: number;
  coordY: number;
  active: boolean;
  createdAt: string;
}

// DeskCreate: schema #/components/schemas/DeskCreate
// (usado también por PUT /desks/{id} vía DeskUpdate; espejo de ParkingSpaceCreate).
export interface DeskCreate {
  number: number;
  category: DeskCategory;
  coordX: number;
  coordY: number;
  active?: boolean;
}

// PageMeta + PageDesk: schemas #/components/schemas/PageMeta y PageDesk.
export interface PageDesk {
  content: Desk[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

// Parámetros de listado: PageParam, SizeParam y filtros active/category.
export interface DeskListParams {
  page?: number;
  size?: number;
  active?: boolean;
  category?: DeskCategory;
}
