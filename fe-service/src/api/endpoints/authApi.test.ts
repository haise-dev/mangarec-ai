import { describe, it, expect, vi } from "vitest";
import {
  login,
  register,
  loginWithGoogle,
  forgotPassword,
  resetPassword,
  resendEmailVerification,
  verifyEmail,
} from "./authApi";
import { publicApiClient } from "@/api/apiClient";

vi.mock("@/api/apiClient", () => ({
  publicApiClient: {
    post: vi.fn(),
  },
}));

describe("authApi", () => {
  it("login", async () => {
    vi.mocked(publicApiClient.post).mockResolvedValue({ data: { data: "token" } });
    expect(await login({ email: "a", password: "b" })).toBe("token");
  });

  it("register", async () => {
    vi.mocked(publicApiClient.post).mockResolvedValue({ data: { data: "user" } });
    expect(await register({ email: "a", password: "b", name: "c" })).toBe("user");
  });

  it("loginWithGoogle", async () => {
    vi.mocked(publicApiClient.post).mockResolvedValue({ data: { data: "token" } });
    expect(await loginWithGoogle({ idToken: "xyz" })).toBe("token");
  });

  it("forgotPassword", async () => {
    vi.mocked(publicApiClient.post).mockResolvedValue({ data: "ok" });
    expect(await forgotPassword({ email: "a" })).toBe("ok");
  });

  it("resetPassword", async () => {
    vi.mocked(publicApiClient.post).mockResolvedValue({ data: "ok" });
    expect(await resetPassword({ email: "a", otp: "b", newPassword: "c", confirmPassword: "c" })).toBe("ok");
  });

  it("resendEmailVerification", async () => {
    vi.mocked(publicApiClient.post).mockResolvedValue({ data: "ok" });
    expect(await resendEmailVerification({ email: "a" })).toBe("ok");
  });

  it("verifyEmail", async () => {
    vi.mocked(publicApiClient.post).mockResolvedValue({ data: "ok" });
    expect(await verifyEmail({ email: "a", otp: "b" })).toBe("ok");
  });
});
