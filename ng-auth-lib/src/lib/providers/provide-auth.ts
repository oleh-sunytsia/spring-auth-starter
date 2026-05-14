import {
  EnvironmentProviders,
  makeEnvironmentProviders,
} from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';

import { AuthConfig, ResolvedAuthConfig } from '../models/auth-config.model';
import { AUTH_CONFIG } from './auth-config.token';
import { AuthService } from '../services/auth.service';
import { TokenStorageService } from '../services/token-storage.service';
import { TokenDecoderService } from '../services/token-decoder.service';
import { jwtBearerInterceptor } from '../interceptors/jwt-bearer.interceptor';
import { authErrorInterceptor } from '../interceptors/auth-error.interceptor';

/**
 * Registers all ng-auth-lib providers in the Angular DI tree.
 *
 * ### Usage — in `main.ts` / `app.config.ts`
 * ```ts
 * bootstrapApplication(AppComponent, {
 *   providers: [
 *     provideRouter(routes),
 *     provideAuth({
 *       apiEndpoint: 'http://localhost:8080',
 *     }),
 *   ],
 * });
 * ```
 */
export function provideAuth(config: AuthConfig): EnvironmentProviders {
  const resolved = resolveConfig(config);

  return makeEnvironmentProviders([
    // Configuration token
    { provide: AUTH_CONFIG, useValue: resolved },

    // Core services
    AuthService,
    TokenStorageService,
    TokenDecoderService,

    // HTTP client with both interceptors in order:
    // 1. jwtBearerInterceptor  → add Authorization header
    // 2. authErrorInterceptor  → handle 401 + refresh
    provideHttpClient(
      withInterceptors([jwtBearerInterceptor, authErrorInterceptor])
    ),
  ]);
}

// ── private ────────────────────────────────────────────────────────────────

function resolveConfig(config: AuthConfig): ResolvedAuthConfig {
  const base  = config.apiEndpoint.replace(/\/$/, '');
  const authBase = `${base}/api/auth`;

  return {
    apiEndpoint:       base,
    loginUrl:          config.loginUrl          ?? `${authBase}/login`,
    refreshUrl:        config.refreshUrl         ?? `${authBase}/refresh`,
    logoutUrl:         config.logoutUrl          ?? `${authBase}/logout`,
    validateUrl:       config.validateUrl        ?? `${authBase}/validate`,
    accessTokenKey:    config.accessTokenKey     ?? 'access_token',
    refreshTokenKey:   config.refreshTokenKey    ?? 'refresh_token',
    whitelistedUrls:   config.whitelistedUrls    ?? [],
    blacklistedUrls:   config.blacklistedUrls    ?? [
      `${authBase}/login`,
      `${authBase}/refresh`,
    ],
    defaultRedirectUrl: config.defaultRedirectUrl ?? '/',
    loginRedirectUrl:   config.loginRedirectUrl   ?? '/login',
  };
}
