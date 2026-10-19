import { publicApiClient } from "@/api/apiClient";
import type { ApiResponse, GuestSessionResponse } from "@/types";
import { storage } from "@/utils/storage";

export async function createGuestSession(): Promise<GuestSessionResponse> {
  const response = await publicApiClient.post<ApiResponse<GuestSessionResponse>>("/api/guests");
  storage.setGuestId(response.data.data.guestId);
  return response.data.data;
}

export async function getOrCreateGuestId(): Promise<string | undefined> {
  const existingGuestId = storage.getGuestId();
  if (existingGuestId) return existingGuestId;

  try {
    const guest = await createGuestSession();
    return guest.guestId;
  } catch {
    return undefined;
  }
}
