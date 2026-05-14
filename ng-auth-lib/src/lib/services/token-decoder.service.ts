import { Injectable } from '@angular/core';

interface JwtHeader {
  alg: string;
  typ: string;
}

interface JwtPayload {
  sub?: string;
  username?: string;
  email?: string;
  roles?: string[];
  permissions?: string[];
  iat?: number;
  exp?: number;
  iss?: string;
  type?: string;
  [key: string]: unknown;
}

/**
 * Decodes JWT tokens client-side (without verifying the signature).
 * Signature verification is the server's responsibility.
 */
@Injectable()
export class TokenDecoderService {

  /**
   * Decode the JWT payload section. Returns null if the token is invalid.
   */
  decodePayload(token: string): JwtPayload | null {
    try {
      const parts = token.split('.');
      if (parts.length !== 3) return null;
      const decoded = this.base64UrlDecode(parts[1]);
      return JSON.parse(decoded) as JwtPayload;
    } catch {
      return null;
    }
  }

  decodeHeader(token: string): JwtHeader | null {
    try {
      const parts = token.split('.');
      if (parts.length !== 3) return null;
      return JSON.parse(this.base64UrlDecode(parts[0])) as JwtHeader;
    } catch {
      return null;
    }
  }

  /** Returns the expiration time as a Date, or null if absent. */
  getExpiration(token: string): Date | null {
    const payload = this.decodePayload(token);
    if (!payload?.exp) return null;
    return new Date(payload.exp * 1000);
  }

  /** True if the token's `exp` claim is in the past. */
  isExpired(token: string): boolean {
    const exp = this.getExpiration(token);
    if (!exp) return true;
    return exp.getTime() < Date.now();
  }

  /** Seconds remaining before expiration (0 if already expired). */
  secondsUntilExpiry(token: string): number {
    const exp = this.getExpiration(token);
    if (!exp) return 0;
    return Math.max(0, Math.floor((exp.getTime() - Date.now()) / 1000));
  }

  getRoles(token: string): string[] {
    return this.decodePayload(token)?.roles ?? [];
  }

  getPermissions(token: string): string[] {
    return this.decodePayload(token)?.permissions ?? [];
  }

  getSubject(token: string): string | null {
    return this.decodePayload(token)?.sub ?? null;
  }

  // ── private ────────────────────────────────────────────────────────────

  private base64UrlDecode(encoded: string): string {
    const base64 = encoded.replace(/-/g, '+').replace(/_/g, '/');
    const padded  = base64.padEnd(base64.length + (4 - (base64.length % 4)) % 4, '=');
    return atob(padded);
  }
}
