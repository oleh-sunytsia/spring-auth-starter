import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import {
  Router,
  ActivatedRouteSnapshot,
  RouterStateSnapshot,
  UrlTree,
  Data,
} from '@angular/router';
import { BehaviorSubject, Observable } from 'rxjs';

import { roleGuard, permissionGuard } from './role.guard';
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

interface MockAuthOptions {
  isAuthenticated: boolean;
  hasAnyRole?: (...roles: string[]) => boolean;
  hasAnyPermission?: (...perms: string[]) => boolean;
}

function buildRoute(routeData: Data): ActivatedRouteSnapshot {
  return { data: routeData } as unknown as ActivatedRouteSnapshot;
}

function setupGuard(opts: MockAuthOptions) {
  const auth$ = new BehaviorSubject(opts.isAuthenticated);
  const mockAuthService = {
    isAuthenticated$: auth$.asObservable(),
    hasAnyRole: opts.hasAnyRole ?? jest.fn().mockReturnValue(false),
    hasAnyPermission: opts.hasAnyPermission ?? jest.fn().mockReturnValue(false),
  };
  const mockRouter = {
    createUrlTree: (commands: string[]) => commands[0] as unknown as UrlTree,
  };

  TestBed.configureTestingModule({
    providers: [
      { provide: AuthService, useValue: mockAuthService },
      { provide: Router, useValue: mockRouter },
      { provide: AUTH_CONFIG, useValue: TEST_CONFIG },
    ],
  });

  return { auth$, mockAuthService };
}

// ── roleGuard ─────────────────────────────────────────────────────────────

describe('roleGuard', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('redirects to /login when not authenticated', fakeAsync(() => {
    setupGuard({ isAuthenticated: false });
    let result: boolean | UrlTree | undefined;

    (TestBed.runInInjectionContext(() =>
      roleGuard(buildRoute({ roles: ['ADMIN'] }), {} as RouterStateSnapshot)
    ) as Observable<boolean | UrlTree>).subscribe(r => (result = r));
    tick();

    expect(result).toBe('/login');
  }));

  it('allows access when authenticated and has required role', fakeAsync(() => {
    setupGuard({ isAuthenticated: true, hasAnyRole: () => true });
    let result: boolean | UrlTree | undefined;

    (TestBed.runInInjectionContext(() =>
      roleGuard(buildRoute({ roles: ['ADMIN'] }), {} as RouterStateSnapshot)
    ) as Observable<boolean | UrlTree>).subscribe(r => (result = r));
    tick();

    expect(result).toBe(true);
  }));

  it('redirects to defaultRedirectUrl when authenticated but missing role', fakeAsync(() => {
    setupGuard({ isAuthenticated: true, hasAnyRole: () => false });
    let result: boolean | UrlTree | undefined;

    (TestBed.runInInjectionContext(() =>
      roleGuard(buildRoute({ roles: ['ADMIN'] }), {} as RouterStateSnapshot)
    ) as Observable<boolean | UrlTree>).subscribe(r => (result = r));
    tick();

    expect(result).toBe('/');
  }));

  it('allows access when no roles required (empty array)', fakeAsync(() => {
    setupGuard({ isAuthenticated: true });
    let result: boolean | UrlTree | undefined;

    (TestBed.runInInjectionContext(() =>
      roleGuard(buildRoute({ roles: [] }), {} as RouterStateSnapshot)
    ) as Observable<boolean | UrlTree>).subscribe(r => (result = r));
    tick();

    expect(result).toBe(true);
  }));
});

// ── permissionGuard ───────────────────────────────────────────────────────

describe('permissionGuard', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('redirects to /login when not authenticated', fakeAsync(() => {
    setupGuard({ isAuthenticated: false });
    let result: boolean | UrlTree | undefined;

    (TestBed.runInInjectionContext(() =>
      permissionGuard(buildRoute({ permissions: ['READ:USERS'] }), {} as RouterStateSnapshot)
    ) as Observable<boolean | UrlTree>).subscribe(r => (result = r));
    tick();

    expect(result).toBe('/login');
  }));

  it('allows access when user has required permission', fakeAsync(() => {
    setupGuard({ isAuthenticated: true, hasAnyPermission: () => true });
    let result: boolean | UrlTree | undefined;

    (TestBed.runInInjectionContext(() =>
      permissionGuard(buildRoute({ permissions: ['READ:USERS'] }), {} as RouterStateSnapshot)
    ) as Observable<boolean | UrlTree>).subscribe(r => (result = r));
    tick();

    expect(result).toBe(true);
  }));

  it('redirects to defaultRedirectUrl when permission missing', fakeAsync(() => {
    setupGuard({ isAuthenticated: true, hasAnyPermission: () => false });
    let result: boolean | UrlTree | undefined;

    (TestBed.runInInjectionContext(() =>
      permissionGuard(buildRoute({ permissions: ['WRITE:ORDERS'] }), {} as RouterStateSnapshot)
    ) as Observable<boolean | UrlTree>).subscribe(r => (result = r));
    tick();

    expect(result).toBe('/');
  }));
});
