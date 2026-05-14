import { inject } from '@angular/core';
import {
  HttpRequest,
  HttpHandlerFn,
  HttpInterceptorFn,
} from '@angular/common/http';

import { TokenStorageService } from '../services/token-storage.service';
import { AUTH_CONFIG } from '../providers/auth-config.token';

/**
 * Functional HTTP interceptor — appends `Authorization: Bearer <token>`
 * to outgoing requests, respecting whitelist / blacklist rules.
 */
export const jwtBearerInterceptor: HttpInterceptorFn = (
  req: HttpRequest<unknown>,
  next: HttpHandlerFn
) => {
  const tokenStorage = inject(TokenStorageService);
  const config       = inject(AUTH_CONFIG);

  const token = tokenStorage.getAccessToken();

  if (!token || shouldSkip(req.url, config.blacklistedUrls, config.whitelistedUrls, config.apiEndpoint)) {
    return next(req);
  }

  const authReq = req.clone({
    setHeaders: { Authorization: `Bearer ${token}` },
  });

  return next(authReq);
};

// ── helpers ──────────────────────────────────────────────────────────────

function shouldSkip(
  url: string,
  blacklist: string[],
  whitelist: string[],
  apiEndpoint: string
): boolean {
  // Always skip blacklisted patterns
  if (matchesAny(url, blacklist)) return true;

  // If whitelist is configured, only attach token to matching URLs
  if (whitelist.length > 0) {
    return !matchesAny(url, whitelist);
  }

  // Default: attach to any URL that targets the configured API endpoint
  return !url.startsWith(apiEndpoint);
}

function matchesAny(url: string, patterns: string[]): boolean {
  return patterns.some(pattern => {
    if (pattern.includes('*')) {
      // Simple glob — convert to regex
      const regex = new RegExp('^' + pattern.replace(/\*/g, '.*') + '$');
      return regex.test(url);
    }
    return url.startsWith(pattern);
  });
}
