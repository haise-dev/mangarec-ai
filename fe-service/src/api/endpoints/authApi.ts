import { publicApiClient } from "@/api/apiClient";
import type {
  ApiResponse,
  AuthUser,
  ForgotPasswordRequest,
  GoogleLoginRequest,
  LoginRequest,
  RegisterRequest,
  ResendEmailVerificationRequest,
  ResetPasswordRequest,
  TokenResponse,
  VerifyEmailRequest,
} from "@/types";

const AUTH_BASE = "/api/v1/auth";

export async function login(payload: LoginRequest): Promise<TokenResponse> {
  const response = await publicApiClient.post<ApiResponse<TokenResponse>>(`${AUTH_BASE}/login`, payload);
  return response.data.data;
}

export async function register(payload: RegisterRequest): Promise<AuthUser> {
  const response = await publicApiClient.post<ApiResponse<AuthUser>>(`${AUTH_BASE}/register`, payload);
  return response.data.data;
}

export async function loginWithGoogle(payload: GoogleLoginRequest): Promise<TokenResponse> {
  const response = await publicApiClient.post<ApiResponse<TokenResponse>>(`${AUTH_BASE}/google`, payload);
  return response.data.data;
}

export async function forgotPassword(payload: ForgotPasswordRequest): Promise<ApiResponse<null>> {
  const response = await publicApiClient.post<ApiResponse<null>>(`${AUTH_BASE}/forgot-password`, payload);
  return response.data;
}

export async function resetPassword(payload: ResetPasswordRequest): Promise<ApiResponse<null>> {
  const response = await publicApiClient.post<ApiResponse<null>>(`${AUTH_BASE}/reset-password`, payload);
  return response.data;
}

export async function resendEmailVerification(
  payload: ResendEmailVerificationRequest,
): Promise<ApiResponse<null>> {
  const response = await publicApiClient.post<ApiResponse<null>>(`${AUTH_BASE}/resend-verification`, payload);
  return response.data;
}

export async function verifyEmail(payload: VerifyEmailRequest): Promise<ApiResponse<null>> {
  const response = await publicApiClient.post<ApiResponse<null>>(`${AUTH_BASE}/verify-email`, payload);
  return response.data;
}
