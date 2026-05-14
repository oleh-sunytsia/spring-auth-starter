import { inject } from '@angular/core';
import { CanActivateFn, Router, ActivatedRouteSnapshot } from '@angular/router';
import { map, take } from 'rxjs';

import { AuthService } from '../services/auth.service';
import { AUTH_CONFIG } from '../providers/auth-config.token';

/**
 * Functional guard — allows access only to users with a specified role.
 *
 * Attach to a route using `data.roles`:
 * ```ts
 * {
 *   path: 'admin',
 *   component: AdminComponent,
 *   canActivate: [roleGuard],
 *   data: { roles: ['ADMIN'] }
 * }
 * ```
 */
export const roleGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const authService = inject(AuthService);
  const router      = inject(Router);
  const config      = inject(AUTH_CONFIG);

  const requiredRoles: string[] = route.data?.['roles'] ?? [];

  return authService.isAuthenticated$.pipe(
    take(1),
    map(isAuth => {
      if (!isAuth) {
        return router.createUrlTree([config.loginRedirectUrl]);
      }

      if (requiredRoles.length === 0) return true;

      const hasRole = authService.hasAnyRole(...requiredRoles);
      if (hasRole) return true;

      // Authenticated but missing role → redirect to default page
      return router.createUrlTree([config.defaultRedirectUrl]);
    })
  );
};

/**
 * Functional guard — allows access only to users with a specified permission.
 *
 * ```ts
 * {
 *   path: 'users',
 *   canActivate: [permissionGuard],
 *   data: { permissions: ['READ:USERS'] }
 * }
 * ```
 */
export const permissionGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const authService  = inject(AuthService);
  const router       = inject(Router);
  const config       = inject(AUTH_CONFIG);

  const required: string[] = route.data?.['permissions'] ?? [];

  return authService.isAuthenticated$.pipe(
    take(1),
    map(isAuth => {
      if (!isAuth) {
        return router.createUrlTree([config.loginRedirectUrl]);
      }
      if (required.length === 0) return true;
      return authService.hasAnyPermission(...required)
        ? true
        : router.createUrlTree([config.defaultRedirectUrl]);
    })
  );
};
