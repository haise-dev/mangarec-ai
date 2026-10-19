import { describe, it, expect, vi, beforeEach } from "vitest";
import { createGuestSession, getOrCreateGuestId } from "./guestApi";
import { publicApiClient } from "@/api/apiClient";
import { storage } from "@/utils/storage";

vi.mock("@/api/apiClient", () => ({
  publicApiClient: {
    post: vi.fn(),
  },
}));

vi.mock("@/utils/storage", () => ({
  storage: {
    getGuestId: vi.fn(),
    setGuestId: vi.fn(),
  },
}));

describe("guestApi", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe("createGuestSession", () => {
    it("should create guest session and save to storage", async () => {
      vi.mocked(publicApiClient.post).mockResolvedValueOnce({
        data: { data: { guestId: "guest-123", expiresAt: "date" } },
      } as any);

      const result = await createGuestSession();
      expect(publicApiClient.post).toHaveBeenCalledWith("/api/guests");
      expect(storage.setGuestId).toHaveBeenCalledWith("guest-123");
      expect(result.guestId).toBe("guest-123");
    });
  });

  describe("getOrCreateGuestId", () => {
    it("should return existing guest id", async () => {
      vi.mocked(storage.getGuestId).mockReturnValue("existing-123");
      const result = await getOrCreateGuestId();
      expect(result).toBe("existing-123");
      expect(publicApiClient.post).not.toHaveBeenCalled();
    });

    it("should create new guest id if not exists", async () => {
      vi.mocked(storage.getGuestId).mockReturnValue(null);
      vi.mocked(publicApiClient.post).mockResolvedValueOnce({
        data: { data: { guestId: "new-guest-123", expiresAt: "date" } },
      } as any);

      const result = await getOrCreateGuestId();
      expect(result).toBe("new-guest-123");
    });

    it("should return undefined on error", async () => {
      vi.mocked(storage.getGuestId).mockReturnValue(null);
      vi.mocked(publicApiClient.post).mockRejectedValueOnce(new Error("API Error"));

      const result = await getOrCreateGuestId();
      expect(result).toBeUndefined();
    });
  });
});
