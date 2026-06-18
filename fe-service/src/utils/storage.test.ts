import { describe, it, expect, beforeEach, vi } from "vitest";
import { storage } from "./storage";
import type { AuthSession } from "@/types";

describe("storage utility", () => {
  beforeEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
  });

  it("should get and set auth", () => {
    const session: AuthSession = {
      accessToken: "access",
      refreshToken: "refresh",
      accessTokenExpiresAt: "expires",
      user: { id: "1", email: "test@test.com", username: "test", roles: [] }
    };

    storage.setAuth(session);
    
    expect(storage.getAccessToken()).toBe("access");
    expect(storage.getRefreshToken()).toBe("refresh");
    expect(storage.getAccessTokenExpiresAt()).toBe("expires");
    expect(storage.getUser()?.email).toBe("test@test.com");
  });

  it("should clear auth", () => {
    storage.setAuth({
      accessToken: "a", refreshToken: "b", accessTokenExpiresAt: "c",
      user: { id: "1", email: "e", username: "u", roles: [] }
    });
    storage.clearAuth();
    expect(storage.getAccessToken()).toBeNull();
    expect(storage.getUser()).toBeNull();
  });

  it("should get and set guest ID", () => {
    storage.setGuestId("guest-123");
    expect(storage.getGuestId()).toBe("guest-123");
  });

  it("should create device id if not exists", () => {
    const deviceId = storage.getOrCreateDeviceId();
    expect(deviceId).toBeTruthy();
    expect(storage.getOrCreateDeviceId()).toBe(deviceId); // should return existing
  });

  it("should return null when parsing invalid user", () => {
    localStorage.setItem("mangarec_user", "{invalid-json}");
    expect(storage.getUser()).toBeNull();
  });
});
