import { AuthUser } from './auth-user.model';

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  /** Expiration in seconds */
  expiresIn: number;
  user: AuthUser;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface ValidateTokenResponse {
  valid: boolean;
  userId?: string;
  roles?: string[];
  permissions?: string[];
}
