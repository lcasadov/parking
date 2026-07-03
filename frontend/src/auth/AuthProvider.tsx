import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useCallback, useEffect, useMemo, type ReactNode } from 'react';
import { AxiosError } from 'axios';
import { getCurrentUser } from '../api/authApi';
import { setSessionActive } from '../api/sessionState';
import type { CurrentUser } from '../types/auth';
import { AuthContext, type AuthContextValue } from './authContext';

export const CURRENT_USER_QUERY_KEY = ['auth', 'me'] as const;

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();

  const { data, isLoading, refetch } = useQuery<CurrentUser | null>({
    queryKey: CURRENT_USER_QUERY_KEY,
    queryFn: async () => {
      try {
        return await getCurrentUser();
      } catch (error) {
        // 401 -> sin sesion: usuario null (no es un error de la UI).
        if (error instanceof AxiosError && error.response?.status === 401) {
          return null;
        }
        throw error;
      }
    },
    retry: false,
    staleTime: 30_000,
  });

  const setUser = useCallback(
    (user: CurrentUser) => queryClient.setQueryData(CURRENT_USER_QUERY_KEY, user),
    [queryClient],
  );

  const clearUser = useCallback(
    () => queryClient.setQueryData(CURRENT_USER_QUERY_KEY, null),
    [queryClient],
  );

  const user = data ?? null;

  // Sincroniza el flag de sesion que consulta el interceptor 401 (bug #9):
  // solo con un usuario autenticado un 401 posterior significa "sesion expirada".
  useEffect(() => {
    setSessionActive(user !== null);
    return () => setSessionActive(false);
  }, [user]);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      isLoading,
      isAuthenticated: user !== null,
      refetch,
      setUser,
      clearUser,
    }),
    [user, isLoading, refetch, setUser, clearUser],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
