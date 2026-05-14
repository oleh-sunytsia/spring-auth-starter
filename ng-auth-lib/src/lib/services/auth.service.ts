import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import {
  BehaviorSubject,
  Observable,
  throwError,
  tap,
  catchError,
  map,
  of,
} from 'rxjs';

import { AUTH_CONFIG } from '../providers/auth-config.token';
import { TokenStorageService } from './token-storage.service';
import { TokenDecoderService } from './token-decoder.service';
import { AuthUser } from '../models/auth-user.model';
import {
  AuthResponse,
  LoginRequest,
  RefreshTokenRequest,
  ValidateTokenResponse,
} from '../models/auth-response.model';

export interface AuthState {
  user: AuthUser | null;
  isAuthenticated: boolean;
  isLoading: boolean;
}

const INITIAL_STATE: AuthState = {
  user: null,
  isAuthenticated: false,
  isLoading: false,
};

/**
 * Core authentication service.
 * Manages auth state via BehaviorSubject and delegates token I/O to TokenStorageService.
 */
@Injectable()
export class AuthService {

  private readonly config          = inject(AUTH_CONFIG);
  private readonly http            = inject(HttpClient);
  private readonly router          = inject(Router);
  private readonly tokenStorage    = inject(TokenStorageService);
  private readonly tokenDecoder    = inject(TokenDecoderService);

  private readonly _state$ = new BehaviorSubject<AuthState>(INITIAL_STATE);

  /** Observe the full auth state. */
  readonly state$: Observable<AuthState> = this._state$.asObservable();

  /** Shorthand: current authenticated user (or null). */
  readonly user$: Observable<AuthUser | null> = this._state$.pipe(
    map(s => s.user)
  );

  /** Shorthand: whether the user is currently logged in. */
  readonly isAuthenticated$: Observable<boolean> = this._state$.pipe(
    map(s => s.isAuthenticated)
  );

  constructor() {
    this.restoreSession();
  }

  // ── Public API ─────────────────────────────────────────────────────────

  login(username: string, password: string): Observable<AuthResponse> {
    this.patchState({ isLoading: true });

    const body: LoginRequest = { username, password };

    return this.http.post<AuthResponse>(this.config.loginUrl, body).pipe(
      tap(res => this.handleAuthResponse(res)),
      catchError(err => {
        this.patchState({ isLoading: false });
        return throwError(() => err);
      })
    );
  }

  refreshToken(): Observable<AuthResponse> {
    const refreshToken = this.tokenStorage.getRefreshToken();
    if (!refreshToken) {
      return throwError(() => new Error('No refresh token available'));
    }

    const body: RefreshTokenRequest = { refreshToken };

    return this.http.post<AuthResponse>(this.config.refreshUrl, body).pipe(
      tap(res => {
        this.tokenStorage.saveAccessToken(res.accessToken);
        if (res.refreshToken) {
          this.tokenStorage.saveRefreshToken(res.refreshToken);
        }
      }),
      catchError(err => {
        this.logout();
        return throwError(() => err);
      })
    );
  }

  logout(): void {
    const refreshToken = this.tokenStorage.getRefreshToken();

    if (refreshToken) {
      const body: RefreshTokenRequest = { refreshToken };
      this.http.post(this.config.logoutUrl, body).pipe(
        catchError(() => of(null))   // best-effort
      ).subscribe();
    }

    this.tokenStorage.clearTokens();
    this._state$.next(INITIAL_STATE);
    this.router.navigateByUrl(this.config.loginRedirectUrl);
  }

  validateToken(token: string): Observable<ValidateTokenResponse> {
    return this.http.get<ValidateTokenResponse>(this.config.validateUrl, {
      headers: { Authorization: `Bearer ${token}` },
    });
  }

  /** Synchronous snapshot of current auth state. */
  getState(): AuthState {
    return this._state$.getValue();
  }

  isAuthenticated(): boolean {
    return this._state$.getValue().isAuthenticated;
  }

  hasRole(role: string): boolean {
    const user = this._state$.getValue().user;
    if (!user) return false;
    const normalized = role.startsWith('ROLE_') ? role : `ROLE_${role}`;
    return user.roles.includes(normalized) || user.roles.includes(role);
  }

  hasAnyRole(...roles: string[]): boolean {
    return roles.some(r => this.hasRole(r));
  }

  hasPermission(permission: string): boolean {
    return this._state$.getValue().user?.permissions?.includes(permission) ?? false;
  }

  hasAnyPermission(...permissions: string[]): boolean {
    return permissions.some(p => this.hasPermission(p));
  }

  // ── Internal ───────────────────────────────────────────────────────────

  /**
   * Called by AuthErrorInterceptor after a successful token refresh
   * to update the stored access token without a full state reset.
   */
  updateAccessToken(newAccessToken: string): void {
    this.tokenStorage.saveAccessToken(newAccessToken);
  }

  private handleAuthResponse(res: AuthResponse): void {
    this.tokenStorage.saveAccessToken(res.accessToken);
    this.tokenStorage.saveRefreshToken(res.refreshToken);
    this.patchState({
      user: res.user,
      isAuthenticated: true,
      isLoading: false,
    });
  }

  private restoreSession(): void {
    const token = this.tokenStorage.getAccessToken();
    if (!token || this.tokenDecoder.isExpired(token)) {
      return;
    }
    const payload = this.tokenDecoder.decodePayload(token);
    if (!payload) return;

    // Reconstruct a minimal AuthUser from token claims
    const user: AuthUser = {
      id:          payload['sub']         as string ?? '',
      username:    payload['username']    as string ?? '',
      email:       payload['email']       as string ?? '',
      roles:       (payload['roles']       as string[]) ?? [],
      permissions: (payload['permissions'] as string[]) ?? [],
    };

    this.patchState({ user, isAuthenticated: true });
  }

  private patchState(partial: Partial<AuthState>): void {
    this._state$.next({ ...this._state$.getValue(), ...partial });
  }
}
