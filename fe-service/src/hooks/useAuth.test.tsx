import { renderHook } from "@testing-library/react";
import { describe, it, expect, vi } from "vitest";
import { useAuth } from "./useAuth";
import { AuthContext } from "@/context/auth-context-value";

describe("useAuth hook", () => {
  it("should throw error if used outside of provider", () => {
    expect(() => renderHook(() => useAuth())).toThrow("useAuth must be used within AuthProvider");
  });

  it("should return context if used within provider", () => {
    const mockContext: any = { user: null, isAuthenticated: false, isLoading: false };
    const wrapper = ({ children }: any) => (
      <AuthContext.Provider value={mockContext}>{children}</AuthContext.Provider>
    );
    const { result } = renderHook(() => useAuth(), { wrapper });
    expect(result.current).toBe(mockContext);
  });
});
