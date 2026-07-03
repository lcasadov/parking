import { createContext } from 'react';
import type { CurrentUser } from '../types/auth';

export interface AuthContextValue {
  user: CurrentUser | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  refetch: () => Promise<unknown>;
  setUser: (user: CurrentUser) => void;
  clearUser: () => void;
}

export const AuthContext = createContext<AuthContextValue | undefined>(undefined);
