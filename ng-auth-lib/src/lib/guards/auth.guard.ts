import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map, take } from 'rxjs';

import { AuthService } from '../services/auth.service';
import { AUTH_CONFIG } from '../providers/auth-config.token';

/**
 * Functional guard — protects routes that require authentication.
 *
 * Usage:
 * ```ts
 * { path: 'dashboard', component: DashboardComponent, canActivate: [authGuard] }
 * ```
 */
export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router      = inject(Router);
  const config      = inject(AUTH_CONFIG);

  return authService.isAuthenticated$.pipe(
    take(1),
    map(isAuth => {
      if (isAuth) return true;
      return router.createUrlTree([config.loginRedirectUrl]);
    })
  );
};
