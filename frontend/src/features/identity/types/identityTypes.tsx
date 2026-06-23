export interface IdentityRequest {}

export interface IdentityResponse {}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  userId: number;
  email: string;
  status: string;
}

export interface AuthUser {
  userId: number;
  email: string;
  status: string;
}
