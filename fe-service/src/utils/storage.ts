import type { AuthSession, AuthUser } from "@/types";

const ACCESS_TOKEN_KEY = "mangarec_access_token";
const REFRESH_TOKEN_KEY = "mangarec_refresh_token";
const TOKEN_EXPIRES_AT_KEY = "mangarec_access_token_expires_at";
const USER_KEY = "mangarec_user";
const GUEST_ID_KEY = "mangarec_guest_id";
const DEVICE_ID_KEY = "mangarec_device_id";

const canUseStorage = () => typeof window !== "undefined" && Boolean(window.localStorage);

export const storage = {
  getAccessToken(): string | null {
    return canUseStorage() ? localStorage.getItem(ACCESS_TOKEN_KEY) : null;
  },
  getRefreshToken(): string | null {
    return canUseStorage() ? localStorage.getItem(REFRESH_TOKEN_KEY) : null;
  },
  getAccessTokenExpiresAt(): string | null {
    return canUseStorage() ? localStorage.getItem(TOKEN_EXPIRES_AT_KEY) : null;
  },
  getUser(): AuthUser | null {
    if (!canUseStorage()) return null;
    const raw = localStorage.getItem(USER_KEY);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as AuthUser;
    } catch {
      storage.clearAuth();
      return null;
    }
  },
  setAuth(session: AuthSession): void {
    if (!canUseStorage()) return;
    localStorage.setItem(ACCESS_TOKEN_KEY, session.accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, session.refreshToken);
    localStorage.setItem(TOKEN_EXPIRES_AT_KEY, session.accessTokenExpiresAt);
    localStorage.setItem(USER_KEY, JSON.stringify(session.user));
  },
  clearAuth(): void {
    if (!canUseStorage()) return;
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem(TOKEN_EXPIRES_AT_KEY);
    localStorage.removeItem(USER_KEY);
  },
  getGuestId(): string | null {
    return canUseStorage() ? localStorage.getItem(GUEST_ID_KEY) : null;
  },
  setGuestId(guestId: string): void {
    if (!canUseStorage()) return;
    localStorage.setItem(GUEST_ID_KEY, guestId);
  },
  getOrCreateDeviceId(): string {
    if (!canUseStorage()) return "browser-device";
    const existing = localStorage.getItem(DEVICE_ID_KEY);
    if (existing) return existing;
    const cryptoId = window.crypto?.randomUUID?.();
    const fallbackId = `browser-${Date.now()}-${Math.random().toString(16).slice(2)}`;
    const deviceId = cryptoId ?? fallbackId;
    localStorage.setItem(DEVICE_ID_KEY, deviceId);
    return deviceId;
  },
};
