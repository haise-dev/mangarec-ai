import { describe, it, expect, vi } from "vitest";
import apiClient, { getApiErrorMessage } from "./apiClient";
import { storage } from "@/utils/storage";

vi.mock("@/utils/storage", () => ({
  storage: {
    getAccessToken: vi.fn(),
    getGuestId: vi.fn(),
  },
}));

// Mock axios.isAxiosError
vi.mock("axios", async () => {
  const actual = await vi.importActual("axios");
  return {
    ...(actual as any),
    default: {
      ...(actual as any).default,
      isAxiosError: (err: any) => err?.isAxiosError === true,
    },
    isAxiosError: (err: any) => err?.isAxiosError === true,
  };
});

describe("apiClient interceptor", () => {
  it("should add Authorization header if token exists", () => {
    vi.mocked(storage.getAccessToken).mockReturnValue("fake-token");
    const reqConfig: any = { headers: {} };
    const interceptor = (apiClient.interceptors.request as any).handlers[0].fulfilled;
    const result = interceptor(reqConfig);
    expect(result.headers.Authorization).toBe("Bearer fake-token");
  });

  it("should add X-Guest-Id header if no token but guestId exists", () => {
    vi.mocked(storage.getAccessToken).mockReturnValue(null);
    vi.mocked(storage.getGuestId).mockReturnValue("guest-123");
    const reqConfig: any = { headers: {} };
    const interceptor = (apiClient.interceptors.request as any).handlers[0].fulfilled;
    const result = interceptor(reqConfig);
    expect(result.headers["X-Guest-Id"]).toBe("guest-123");
  });

  it("should do nothing if neither exists", () => {
    vi.mocked(storage.getAccessToken).mockReturnValue(null);
    vi.mocked(storage.getGuestId).mockReturnValue(null);
    const reqConfig: any = { headers: {} };
    const interceptor = (apiClient.interceptors.request as any).handlers[0].fulfilled;
    const result = interceptor(reqConfig);
    expect(result.headers.Authorization).toBeUndefined();
    expect(result.headers["X-Guest-Id"]).toBeUndefined();
  });
});

describe("getApiErrorMessage", () => {
  it("should return message from Error instance", () => {
    expect(getApiErrorMessage(new Error("Test error"))).toBe("Test error");
  });

  it("should return fallback for unknown errors", () => {
    expect(getApiErrorMessage("string error", "fallback")).toBe("fallback");
  });

  it("should handle axios rate limit error", () => {
    const error: any = {
      isAxiosError: true,
      response: { status: 429, headers: { "retry-after": "10" } },
    };
    expect(getApiErrorMessage(error)).toBe("Bạn đang gửi quá nhanh. Vui lòng thử lại sau 10 giây.");
  });

  it("should handle axios rate limit error without retry-after", () => {
    const error: any = {
      isAxiosError: true,
      response: { status: 429 },
    };
    expect(getApiErrorMessage(error)).toBe("Bạn đã dùng hết lượt hôm nay. Vui lòng thử lại sau.");
  });
  
  it("should handle axios error with generic message", () => {
    const error: any = {
      isAxiosError: true,
      response: { data: { message: "Server error" } },
    };
    expect(getApiErrorMessage(error)).toBe("Server error");
  });
});
