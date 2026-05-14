import { Injectable, inject } from '@angular/core';
import { AUTH_CONFIG } from '../providers/auth-config.token';

/**
 * Stores and retrieves JWT tokens from localStorage.
 * Strategy pattern — swap this bean for a cookie-based implementation if needed.
 */
@Injectable()
export class TokenStorageService {

  private readonly config = inject(AUTH_CONFIG);

  get accessTokenKey(): string {
    return this.config.accessTokenKey;
  }

  get refreshTokenKey(): string {
    return this.config.refreshTokenKey;
  }

  // ── Access Token ───────────────────────────────────────────────────────

  saveAccessToken(token: string): void {
    localStorage.setItem(this.accessTokenKey, token);
  }

  getAccessToken(): string | null {
    return localStorage.getItem(this.accessTokenKey);
  }

  removeAccessToken(): void {
    localStorage.removeItem(this.accessTokenKey);
  }

  // ── Refresh Token ──────────────────────────────────────────────────────

  saveRefreshToken(token: string): void {
    localStorage.setItem(this.refreshTokenKey, token);
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(this.refreshTokenKey);
  }

  removeRefreshToken(): void {
    localStorage.removeItem(this.refreshTokenKey);
  }

  // ── Convenience ────────────────────────────────────────────────────────

  clearTokens(): void {
    this.removeAccessToken();
    this.removeRefreshToken();
  }

  hasAccessToken(): boolean {
    return this.getAccessToken() !== null;
  }
}
