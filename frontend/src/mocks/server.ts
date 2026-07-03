import { setupServer } from 'msw/node';
import { handlers } from './handlers';

// Servidor MSW para el entorno de tests (Node).
export const server = setupServer(...handlers);
