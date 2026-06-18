import { renderHook, act } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { AuthProvider } from "./AuthContext";
import { useAuth } from "@/hooks/useAuth";
import { storage } from "@/utils/storage";
import { login, loginWithGoogle, register } from "@/api/endpoints/authApi";
import { getOrCreateGuestId } from "@/api/endpoints/guestApi";

vi.mock("@/utils/storage", () => ({
  storage: {
    getAccessToken: vi.fn(),
    getRefreshToken: vi.fn(),
    getAccessTokenExpiresAt: vi.fn(),
    getUser: vi.fn(),
    clearAuth: vi.fn(),
    setAuth: vi.fn(),
    getOrCreateDeviceId: vi.fn(),
  },
}));

vi.mock("@/utils/jwt", () => ({
  isTokenExpired: vi.fn().mockReturnValue(false),
}));

vi.mock("@/api/endpoints/authApi", () => ({
  login: vi.fn(),
  loginWithGoogle: vi.fn(),
  register: vi.fn(),
}));

vi.mock("@/api/endpoints/guestApi", () => ({
  getOrCreateGuestId: vi.fn(),
}));

describe("AuthContext", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <AuthProvider>{children}</AuthProvider>
  );

  it("should initialize with empty state if no token", () => {
    vi.mocked(storage.getAccessToken).mockReturnValue(null);
    const { result } = renderHook(() => useAuth(), { wrapper });

    expect(result.current.isAuthenticated).toBe(false);
    expect(result.current.user).toBeNull();
    expect(result.current.isLoading).toBe(false);
    expect(storage.clearAuth).toHaveBeenCalled();
  });

  it("should initialize with session if token exists and valid", () => {
    vi.mocked(storage.getAccessToken).mockReturnValue("token");
    vi.mocked(storage.getRefreshToken).mockReturnValue("refresh");
    vi.mocked(storage.getAccessTokenExpiresAt).mockReturnValue("time");
    vi.mocked(storage.getUser).mockReturnValue({ 
      id: 1, 
      name: "Haise", 
      email: "a@b.c", 
      subscriptionStatus: "FREE", 
      emailVerified: true,
      roles: ["USER"] 
    } as any);

    const { result } = renderHook(() => useAuth(), { wrapper });

    expect(result.current.isAuthenticated).toBe(true);
    expect(result.current.user?.name).toBe("Haise");
  });

  it("should handle login", async () => {
    vi.mocked(storage.getAccessToken).mockReturnValue(null);
    vi.mocked(storage.getOrCreateDeviceId).mockReturnValue("dev-1");
    const mockUser = { name: "Test" } as any;
    vi.mocked(login).mockResolvedValue({
      tokenType: "Bearer", accessToken: "access",
      refreshToken: "refresh",
      accessTokenExpiresAt: "expires",
      user: mockUser,
    });

    const { result } = renderHook(() => useAuth(), { wrapper });

    await act(async () => {
      await result.current.login({ email: "a@b.c", password: "pwd" });
    });

    expect(login).toHaveBeenCalledWith({ email: "a@b.c", password: "pwd", deviceId: "dev-1" });
    expect(storage.setAuth).toHaveBeenCalledWith({
      tokenType: "Bearer", accessToken: "access",
      refreshToken: "refresh",
      accessTokenExpiresAt: "expires",
      user: mockUser,
    });
    expect(result.current.isAuthenticated).toBe(true);
    expect(result.current.user).toBe(mockUser);
  });

  it("should handle register", async () => {
    vi.mocked(storage.getAccessToken).mockReturnValue(null);
    vi.mocked(getOrCreateGuestId).mockResolvedValue("guest-1");
    const mockUser = { name: "Test" } as any;
    vi.mocked(register).mockResolvedValue(mockUser);

    const { result } = renderHook(() => useAuth(), { wrapper });

    let regResult;
    await act(async () => {
      regResult = await result.current.register({ email: "a@b.c", password: "pwd", name: "Test" });
    });

    expect(register).toHaveBeenCalledWith({ email: "a@b.c", password: "pwd", name: "Test", guestId: "guest-1" });
    expect(regResult).toBe(mockUser);
  });

  it("should handle loginWithGoogle", async () => {
    vi.mocked(storage.getAccessToken).mockReturnValue(null);
    vi.mocked(storage.getOrCreateDeviceId).mockReturnValue("dev-1");
    vi.mocked(getOrCreateGuestId).mockResolvedValue("guest-1");
    
    const mockUser = { name: "Test Google" } as any;
    vi.mocked(loginWithGoogle).mockResolvedValue({
      tokenType: "Bearer", accessToken: "access-g",
      refreshToken: "refresh-g",
      accessTokenExpiresAt: "expires-g",
      user: mockUser,
    });

    const { result } = renderHook(() => useAuth(), { wrapper });

    await act(async () => {
      await result.current.loginWithGoogle("google-token");
    });

    expect(loginWithGoogle).toHaveBeenCalledWith({ idToken: "google-token", guestId: "guest-1", deviceId: "dev-1" });
    expect(storage.setAuth).toHaveBeenCalled();
    expect(result.current.isAuthenticated).toBe(true);
  });

  it("should handle logout", () => {
    vi.mocked(storage.getAccessToken).mockReturnValue("token");
    vi.mocked(storage.getRefreshToken).mockReturnValue("refresh");
    vi.mocked(storage.getAccessTokenExpiresAt).mockReturnValue("time");
    vi.mocked(storage.getUser).mockReturnValue({ name: "Haise" } as any);

    const { result } = renderHook(() => useAuth(), { wrapper });

    expect(result.current.isAuthenticated).toBe(true);

    act(() => {
      result.current.logout();
    });

    expect(storage.clearAuth).toHaveBeenCalled();
    expect(result.current.isAuthenticated).toBe(false);
  });
});
