import { describe, it, expect } from "vitest";
import { decodeJwt, isTokenExpired } from "./jwt";

describe("jwt utility", () => {
  const createToken = (payload: any) => {
    const header = btoa(JSON.stringify({ alg: "HS256" }));
    const body = btoa(JSON.stringify(payload));
    return `${header}.${body}.signature`;
  };

  it("should decode valid jwt", () => {
    const token = createToken({ sub: "user1", exp: 9999999999 });
    const payload = decodeJwt(token);
    expect(payload?.sub).toBe("user1");
    expect(payload?.exp).toBe(9999999999);
  });

  it("should return null for invalid jwt", () => {
    expect(decodeJwt("invalid-token")).toBeNull();
    expect(decodeJwt("invalid.token.string")).toBeNull();
  });

  it("should correctly identify expired token", () => {
    const past = Math.floor(Date.now() / 1000) - 3600;
    const token = createToken({ exp: past });
    expect(isTokenExpired(token)).toBe(true);
  });

  it("should correctly identify non-expired token", () => {
    const future = Math.floor(Date.now() / 1000) + 3600;
    const token = createToken({ exp: future });
    expect(isTokenExpired(token)).toBe(false);
  });

  it("should consider token without exp as expired", () => {
    const token = createToken({ sub: "user" });
    expect(isTokenExpired(token)).toBe(true);
  });
});
