import { useCallback, useEffect, useMemo, useState } from "react";
import type { ReactNode } from "react";
import {
  login as loginRequest,
  loginWithGoogle as googleLoginRequest,
  register as registerRequest,
} from "@/api/endpoints/authApi";
import { getOrCreateGuestId } from "@/api/endpoints/guestApi";
import { AuthContext } from "@/context/auth-context-value";
import type { AuthContextValue, AuthState } from "@/context/auth-context-value";
import type { AuthSession, LoginRequest, RegisterRequest } from "@/types";
import { isTokenExpired } from "@/utils/jwt";
import { storage } from "@/utils/storage";

const initialState: AuthState = {
  user: null,
  accessToken: null,
  refreshToken: null,
  accessTokenExpiresAt: null,
  isAuthenticated: false,
  isLoading: true,
};

function sessionToState(session: AuthSession): AuthState {
  return {
    user: session.user,
    accessToken: session.accessToken,
    refreshToken: session.refreshToken,
    accessTokenExpiresAt: session.accessTokenExpiresAt,
    isAuthenticated: true,
    isLoading: false,
  };
}

function emptyState(): AuthState {
  return {
    ...initialState,
    isLoading: false,
  };
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<AuthState>(initialState);

  useEffect(() => {
    const accessToken = storage.getAccessToken();
    const refreshToken = storage.getRefreshToken();
    const accessTokenExpiresAt = storage.getAccessTokenExpiresAt();
    const user = storage.getUser();

    if (!accessToken || !refreshToken || !accessTokenExpiresAt || !user || isTokenExpired(accessToken)) {
      storage.clearAuth();
      setState(emptyState());
      return;
    }

    setState(
      sessionToState({
        accessToken,
        refreshToken,
        accessTokenExpiresAt,
        user,
      }),
    );
  }, []);

  const persistSession = useCallback((session: AuthSession) => {
    storage.setAuth(session);
    setState(sessionToState(session));
  }, []);

  const login = useCallback(
    async (payload: Omit<LoginRequest, "deviceId">) => {
      const tokenResponse = await loginRequest({
        ...payload,
        deviceId: storage.getOrCreateDeviceId(),
      });

      persistSession({
        accessToken: tokenResponse.accessToken,
        refreshToken: tokenResponse.refreshToken,
        accessTokenExpiresAt: tokenResponse.accessTokenExpiresAt,
        user: tokenResponse.user,
      });
    },
    [persistSession],
  );

  const register = useCallback(async (payload: Omit<RegisterRequest, "guestId">) => {
    const guestId = await getOrCreateGuestId();
    return registerRequest({
      ...payload,
      guestId,
    });
  }, []);

  const loginWithGoogle = useCallback(
    async (idToken: string) => {
      const guestId = await getOrCreateGuestId();
      const tokenResponse = await googleLoginRequest({
        idToken,
        guestId,
        deviceId: storage.getOrCreateDeviceId(),
      });

      persistSession({
        accessToken: tokenResponse.accessToken,
        refreshToken: tokenResponse.refreshToken,
        accessTokenExpiresAt: tokenResponse.accessTokenExpiresAt,
        user: tokenResponse.user,
      });
    },
    [persistSession],
  );

  const logout = useCallback(() => {
    storage.clearAuth();
    setState(emptyState());
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      ...state,
      login,
      register,
      loginWithGoogle,
      logout,
    }),
    [login, loginWithGoogle, logout, register, state],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

