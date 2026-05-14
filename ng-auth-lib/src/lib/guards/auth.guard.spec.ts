import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Router } from '@angular/router';
import { ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree } from '@angular/router';
import { BehaviorSubject, Observable } from 'rxjs';

import { authGuard } from './auth.guard';
import { AuthService } from '../services/auth.service';
import { AUTH_CONFIG } from '../providers/auth-config.token';
import { ResolvedAuthConfig } from '../models/auth-config.model';

// ── Helpers ───────────────────────────────────────────────────────────────

const TEST_CONFIG: ResolvedAuthConfig = {
  apiEndpoint:        'http://api.test',
  loginUrl:           'http://api.test/api/auth/login',
  refreshUrl:         'http://api.test/api/auth/refresh',
  logoutUrl:          'http://api.test/api/auth/logout',
  validateUrl:        'http://api.test/api/auth/validate',
  accessTokenKey:     'at',
  refreshTokenKey:    'rt',
  whitelistedUrls:    [],
  blacklistedUrls:    [],
  defaultRedirectUrl: '/',
  loginRedirectUrl:   '/login',
};

function runGuard(
  isAuthenticated$: BehaviorSubject<boolean>
): Observable<boolean | UrlTree> {
  const mockAuthService = { isAuthenticated$: isAuthenticated$.asObservable() };
  const routerUrl = (path: string) => ({ toString: () => path } as UrlTree);
  const mockRouter = {
    createUrlTree: (commands: string[]) => routerUrl(commands[0]),
  };

  TestBed.configureTestingModule({
    providers: [
      { provide: AuthService, useValue: mockAuthService },
      { provide: Router, useValue: mockRouter },
      { provide: AUTH_CONFIG, useValue: TEST_CONFIG },
    ],
  });

  return TestBed.runInInjectionContext(() =>
    authGuard(
      {} as ActivatedRouteSnapshot,
      {} as RouterStateSnapshot
    )
  ) as Observable<boolean | UrlTree>;
}

// ── Tests ─────────────────────────────────────────────────────────────────

describe('authGuard', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('allows activation when user is authenticated', fakeAsync(() => {
    const auth$ = new BehaviorSubject(true);
    let result: boolean | UrlTree | undefined;

    runGuard(auth$).subscribe(r => (result = r));
    tick();

    expect(result).toBe(true);
  }));

  it('redirects to loginRedirectUrl when not authenticated', fakeAsync(() => {
    const auth$ = new BehaviorSubject(false);
    let result: boolean | UrlTree | undefined;

    runGuard(auth$).subscribe(r => (result = r));
    tick();

    expect(result).not.toBe(true);
    expect(result?.toString()).toBe('/login');
  }));
});
