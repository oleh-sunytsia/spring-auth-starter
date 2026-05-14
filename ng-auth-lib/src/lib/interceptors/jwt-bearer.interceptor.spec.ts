import { TestBed } from '@angular/core/testing';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { HttpClient } from '@angular/common/http';
import { provideHttpClient, withInterceptors } from '@angular/common/http';

import { jwtBearerInterceptor } from './jwt-bearer.interceptor';
import { TokenStorageService } from '../services/token-storage.service';
import { AUTH_CONFIG } from '../providers/auth-config.token';
import { ResolvedAuthConfig } from '../models/auth-config.model';

// ── Config variants ───────────────────────────────────────────────────────

function makeConfig(overrides: Partial<ResolvedAuthConfig> = {}): ResolvedAuthConfig {
  return {
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
    ...overrides,
  };
}

// ── Shared setup ──────────────────────────────────────────────────────────

function setup(config: ResolvedAuthConfig, storedToken: string | null) {
  const mockTokenStorage = {
    getAccessToken: jest.fn().mockReturnValue(storedToken),
  };

  TestBed.configureTestingModule({
    providers: [
      { provide: AUTH_CONFIG, useValue: config },
      { provide: TokenStorageService, useValue: mockTokenStorage },
      provideHttpClient(withInterceptors([jwtBearerInterceptor])),
      provideHttpClientTesting(),
    ],
  });

  return {
    http: TestBed.inject(HttpClient),
    httpMock: TestBed.inject(HttpTestingController),
  };
}

// ── Tests ─────────────────────────────────────────────────────────────────

describe('jwtBearerInterceptor', () => {
  afterEach(() => {
    TestBed.inject(HttpTestingController).verify();
    TestBed.resetTestingModule();
  });

  it('adds Authorization header for requests to apiEndpoint', () => {
    const { http, httpMock } = setup(makeConfig(), 'my-access-token');

    http.get('http://api.test/data').subscribe();

    const req = httpMock.expectOne('http://api.test/data');
    expect(req.request.headers.get('Authorization')).toBe('Bearer my-access-token');
    req.flush({});
  });

  it('does NOT add header when no token is stored', () => {
    const { http, httpMock } = setup(makeConfig(), null);

    http.get('http://api.test/data').subscribe();

    const req = httpMock.expectOne('http://api.test/data');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('does NOT add header for external URLs (outside apiEndpoint)', () => {
    const { http, httpMock } = setup(makeConfig(), 'my-token');

    http.get('https://external.com/data').subscribe();

    const req = httpMock.expectOne('https://external.com/data');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('skips blacklisted URLs even within apiEndpoint', () => {
    const config = makeConfig({
      blacklistedUrls: ['http://api.test/api/auth/login'],
    });
    const { http, httpMock } = setup(config, 'my-token');

    http.post('http://api.test/api/auth/login', {}).subscribe();

    const req = httpMock.expectOne('http://api.test/api/auth/login');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('only adds header to whitelisted URLs when whitelist is configured', () => {
    const config = makeConfig({
      whitelistedUrls: ['http://api.test/protected/*'],
    });
    const { http, httpMock } = setup(config, 'tok');

    // ── Whitelisted URL — should get header
    http.get('http://api.test/protected/resource').subscribe();
    const matchedReq = httpMock.expectOne('http://api.test/protected/resource');
    expect(matchedReq.request.headers.get('Authorization')).toBe('Bearer tok');
    matchedReq.flush({});

    // ── Non-whitelisted URL — should NOT get header
    http.get('http://api.test/public').subscribe();
    const publicReq = httpMock.expectOne('http://api.test/public');
    expect(publicReq.request.headers.has('Authorization')).toBe(false);
    publicReq.flush({});
  });

  it('supports glob patterns in blacklistedUrls', () => {
    const config = makeConfig({
      blacklistedUrls: ['http://api.test/api/auth/*'],
    });
    const { http, httpMock } = setup(config, 'tok');

    http.post('http://api.test/api/auth/refresh', {}).subscribe();

    const req = httpMock.expectOne('http://api.test/api/auth/refresh');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });
});
