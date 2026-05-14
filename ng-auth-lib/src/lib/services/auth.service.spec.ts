import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { Router } from '@angular/router';

import { AuthService } from './auth.service';
import { TokenStorageService } from './token-storage.service';
import { TokenDecoderService } from './token-decoder.service';
import { AUTH_CONFIG } from '../providers/auth-config.token';
import { ResolvedAuthConfig } from '../models/auth-config.model';
import { AuthResponse } from '../models/auth-response.model';

// ── Helpers ───────────────────────────────────────────────────────────────

function makeJwt(payload: Record<string, unknown>): string {
  const b64 = (obj: unknown) =>
    btoa(JSON.stringify(obj)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=/g, '');
  return `${b64({ alg: 'HS256', typ: 'JWT' })}.${b64(payload)}.sig`;
}

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

const MOCK_USER = {
  id: 'uid-1',
  username: 'alice',
  email: 'alice@test.com',
  roles: ['ROLE_USER'],
  permissions: ['READ:USERS'],
};

const MOCK_AUTH_RESPONSE: AuthResponse = {
  accessToken: 'acc-token',
  refreshToken: 'ref-token',
  expiresIn: 3600,
  user: MOCK_USER,
};

// ── Tests ─────────────────────────────────────────────────────────────────

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let routerSpy: jest.SpyInstance;

  beforeEach(() => {
    localStorage.clear();

    const routerMock = { navigateByUrl: jest.fn() };

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        AuthService,
        TokenStorageService,
        TokenDecoderService,
        { provide: AUTH_CONFIG, useValue: TEST_CONFIG },
        { provide: Router, useValue: routerMock },
      ],
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    routerSpy = jest.spyOn(TestBed.inject(Router), 'navigateByUrl');
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  // ── login ───────────────────────────────────────────────────────────────

  describe('login()', () => {
    it('posts credentials and updates auth state on success', fakeAsync(() => {
      let response: AuthResponse | undefined;

      service.login('alice', 'pass').subscribe(r => (response = r));

      const req = httpMock.expectOne(TEST_CONFIG.loginUrl);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ username: 'alice', password: 'pass' });
      req.flush(MOCK_AUTH_RESPONSE);
      tick();

      expect(response).toEqual(MOCK_AUTH_RESPONSE);

      const state = service.getState();
      expect(state.isAuthenticated).toBe(true);
      expect(state.user).toEqual(MOCK_USER);
      expect(state.isLoading).toBe(false);
    }));

    it('stores tokens in localStorage on success', fakeAsync(() => {
      service.login('alice', 'pass').subscribe();

      httpMock.expectOne(TEST_CONFIG.loginUrl).flush(MOCK_AUTH_RESPONSE);
      tick();

      expect(localStorage.getItem('at')).toBe('acc-token');
      expect(localStorage.getItem('rt')).toBe('ref-token');
    }));

    it('emits error and resets loading flag on failure', fakeAsync(() => {
      let err: unknown;
      service.login('x', 'y').subscribe({ error: e => (err = e) });

      httpMock.expectOne(TEST_CONFIG.loginUrl).flush('Unauthorized', {
        status: 401,
        statusText: 'Unauthorized',
      });
      tick();

      expect(err).toBeTruthy();
      expect(service.getState().isLoading).toBe(false);
    }));
  });

  // ── logout ──────────────────────────────────────────────────────────────

  describe('logout()', () => {
    it('clears tokens and resets state', fakeAsync(() => {
      // Seed a refresh token so logout posts to the server
      localStorage.setItem('rt', 'ref-token');

      service.logout();

      // Flush the fire-and-forget logout request
      httpMock.expectOne(TEST_CONFIG.logoutUrl).flush(null);
      tick();

      expect(localStorage.getItem('at')).toBeNull();
      expect(localStorage.getItem('rt')).toBeNull();
      expect(service.getState().isAuthenticated).toBe(false);
      expect(service.getState().user).toBeNull();
    }));

    it('redirects to loginRedirectUrl', fakeAsync(() => {
      localStorage.setItem('rt', 'ref');
      service.logout();
      httpMock.expectOne(TEST_CONFIG.logoutUrl).flush(null);
      tick();
      expect(routerSpy).toHaveBeenCalledWith('/login');
    }));

    it('skips the server call when no refresh token', () => {
      service.logout();
      httpMock.expectNone(TEST_CONFIG.logoutUrl);
      expect(service.getState().isAuthenticated).toBe(false);
    });
  });

  // ── hasRole / hasPermission ─────────────────────────────────────────────

  describe('hasRole()', () => {
    beforeEach(fakeAsync(() => {
      service.login('a', 'p').subscribe();
      httpMock.expectOne(TEST_CONFIG.loginUrl).flush(MOCK_AUTH_RESPONSE);
      tick();
    }));

    it('returns true for an existing role', () => {
      expect(service.hasRole('ROLE_USER')).toBe(true);
    });

    it('returns true with ROLE_ prefix auto-prepended', () => {
      expect(service.hasRole('USER')).toBe(true);
    });

    it('returns false for an unknown role', () => {
      expect(service.hasRole('ADMIN')).toBe(false);
    });
  });

  describe('hasPermission()', () => {
    beforeEach(fakeAsync(() => {
      service.login('a', 'p').subscribe();
      httpMock.expectOne(TEST_CONFIG.loginUrl).flush(MOCK_AUTH_RESPONSE);
      tick();
    }));

    it('returns true for an existing permission', () => {
      expect(service.hasPermission('READ:USERS')).toBe(true);
    });

    it('returns false for an unknown permission', () => {
      expect(service.hasPermission('WRITE:ORDERS')).toBe(false);
    });
  });

  // ── restoreSession ──────────────────────────────────────────────────────

  describe('restoreSession()', () => {
    it('restores auth state from a valid stored token', () => {
      const futureExp = Math.floor(Date.now() / 1000) + 3600;
      const payload = {
        sub: 'uid-2',
        username: 'bob',
        email: 'bob@test.com',
        roles: ['ROLE_ADMIN'],
        permissions: ['WRITE:ALL'],
        exp: futureExp,
      };
      const token = makeJwt(payload);
      localStorage.setItem('at', token);

      // Re-create service so constructor runs restoreSession()
      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        imports: [HttpClientTestingModule],
        providers: [
          AuthService,
          TokenStorageService,
          TokenDecoderService,
          { provide: AUTH_CONFIG, useValue: TEST_CONFIG },
          { provide: Router, useValue: { navigateByUrl: jest.fn() } },
        ],
      });

      const restored = TestBed.inject(AuthService);
      expect(restored.getState().isAuthenticated).toBe(true);
      expect(restored.getState().user?.username).toBe('bob');
    });

    it('does not restore state from an expired token', () => {
      const pastExp = Math.floor(Date.now() / 1000) - 60;
      const token = makeJwt({ sub: 'uid-3', exp: pastExp });
      localStorage.setItem('at', token);

      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        imports: [HttpClientTestingModule],
        providers: [
          AuthService,
          TokenStorageService,
          TokenDecoderService,
          { provide: AUTH_CONFIG, useValue: TEST_CONFIG },
          { provide: Router, useValue: { navigateByUrl: jest.fn() } },
        ],
      });

      const fresh = TestBed.inject(AuthService);
      expect(fresh.getState().isAuthenticated).toBe(false);
    });
  });
});
