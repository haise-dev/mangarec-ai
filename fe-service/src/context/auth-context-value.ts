import { createContext } from "react";
import type { AuthUser, LoginRequest, RegisterRequest } from "@/types";

export interface AuthState {
  user: AuthUser | null;
  accessToken: string | null;
  refreshToken: string | null;
  accessTokenExpiresAt: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
}

export interface AuthContextValue extends AuthState {
  login: (payload: Omit<LoginRequest, "deviceId">) => Promise<void>;
  register: (payload: Omit<RegisterRequest, "guestId">) => Promise<AuthUser>;
  loginWithGoogle: (idToken: string) => Promise<void>;
  logout: () => void;
}

export const AuthContext = createContext<AuthContextValue | undefined>(undefined);
