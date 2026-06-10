export type UserRole = "USER" | "ADMIN" | string;
export type UserStatus = "ACTIVE" | "DISABLED" | "DELETED" | string;
export type SubscriptionStatus = "FREE" | "PRO" | string;

export interface AuthUser {
  id: string;
  email: string;
  name: string;
  role: UserRole;
  status: UserStatus;
  subscriptionStatus: SubscriptionStatus;
  emailVerified: boolean;
  createdAt: string;
}

export interface LoginRequest {
  email: string;
  password: string;
  deviceId?: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  name: string;
  guestId?: string;
}

export interface GoogleLoginRequest {
  idToken: string;
  guestId?: string;
  deviceId?: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  email: string;
  otp: string;
  newPassword: string;
  confirmPassword: string;
}

export interface ResendEmailVerificationRequest {
  email: string;
}

export interface VerifyEmailRequest {
  email: string;
  otp: string;
}

export interface TokenResponse {
  tokenType: "Bearer" | string;
  accessToken: string;
  refreshToken: string;
  accessTokenExpiresAt: string;
  user: AuthUser;
}

export interface AuthSession {
  accessToken: string;
  refreshToken: string;
  accessTokenExpiresAt: string;
  user: AuthUser;
}

export interface GuestSessionResponse {
  guestId: string;
  cookieName: string;
  expiresAt: string;
}

export interface JwtPayload {
  sub: string;
  email?: string;
  role?: string;
  token_type?: string;
  iat?: number;
  exp?: number;
}
