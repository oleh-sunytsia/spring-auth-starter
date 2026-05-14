import { inject } from '@angular/core';
import {
  HttpRequest,
  HttpHandlerFn,
  HttpInterceptorFn,
  HttpErrorResponse,
  HttpEvent,
} from '@angular/common/http';
import {
  throwError,
  BehaviorSubject,
  Observable,
  switchMap,
  filter,
  take,
  catchError,
} from 'rxjs';

import { AuthService } from '../services/auth.service';
import { TokenStorageService } from '../services/token-storage.service';
import { AuthResponse } from '../models/auth-response.model';

// Shared refresh state across concurrent requests
let isRefreshing = false;
const refreshTokenSubject$ = new BehaviorSubject<string | null>(null);

/**
 * Functional HTTP interceptor.
 * On HTTP 401:
 *   1. Triggers a token refresh (one concurrent refresh at a time).
 *   2. Retries the original request with the new access token.
 *   3. If refresh fails → calls logout().
 */
export const authErrorInterceptor: HttpInterceptorFn = (
  req: HttpRequest<unknown>,
  next: HttpHandlerFn
) => {
  const authService   = inject(AuthService);
  const tokenStorage  = inject(TokenStorageService);

  return next(req).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401) {
        return handle401(req, next, authService, tokenStorage);
      }
      return throwError(() => error);
    })
  );
};

// ── private ───────────────────────────────────────────────────────────────

function handle401(
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
  authService: AuthService,
  tokenStorage: TokenStorageService
): Observable<HttpEvent<unknown>> {
  if (isRefreshing) {
    // Queue until the ongoing refresh completes
    return refreshTokenSubject$.pipe(
      filter((token): token is string => token !== null),
      take(1),
      switchMap(token => next(addBearer(req, token)))
    );
  }

  isRefreshing = true;
  refreshTokenSubject$.next(null);

  return authService.refreshToken().pipe(
    switchMap((res: AuthResponse) => {
      isRefreshing = false;
      refreshTokenSubject$.next(res.accessToken);
      authService.updateAccessToken(res.accessToken);
      return next(addBearer(req, res.accessToken));
    }),
    catchError(err => {
      isRefreshing = false;
      authService.logout();
      return throwError(() => err);
    })
  );
}

function addBearer(req: HttpRequest<unknown>, token: string): HttpRequest<unknown> {
  return req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
}
