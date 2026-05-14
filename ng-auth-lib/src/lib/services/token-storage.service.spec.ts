import { TestBed } from '@angular/core/testing';
import { TokenStorageService } from './token-storage.service';
import { AUTH_CONFIG } from '../providers/auth-config.token';
import { ResolvedAuthConfig } from '../models/auth-config.model';

const TEST_CONFIG: ResolvedAuthConfig = {
  apiEndpoint:        'http://localhost:8080',
  loginUrl:           'http://localhost:8080/api/auth/login',
  refreshUrl:         'http://localhost:8080/api/auth/refresh',
  logoutUrl:          'http://localhost:8080/api/auth/logout',
  validateUrl:        'http://localhost:8080/api/auth/validate',
  accessTokenKey:     'test_access_token',
  refreshTokenKey:    'test_refresh_token',
  whitelistedUrls:    [],
  blacklistedUrls:    [],
  defaultRedirectUrl: '/',
  loginRedirectUrl:   '/login',
};

describe('TokenStorageService', () => {
  let service: TokenStorageService;

  beforeEach(() => {
    localStorage.clear();

    TestBed.configureTestingModule({
      providers: [
        TokenStorageService,
        { provide: AUTH_CONFIG, useValue: TEST_CONFIG },
      ],
    });

    service = TestBed.inject(TokenStorageService);
  });

  afterEach(() => localStorage.clear());

  describe('access token', () => {
    it('saves and retrieves an access token', () => {
      service.saveAccessToken('access-abc');
      expect(service.getAccessToken()).toBe('access-abc');
    });

    it('returns null when no access token stored', () => {
      expect(service.getAccessToken()).toBeNull();
    });

    it('removes the access token', () => {
      service.saveAccessToken('access-abc');
      service.removeAccessToken();
      expect(service.getAccessToken()).toBeNull();
    });

    it('hasAccessToken() returns true when token present', () => {
      service.saveAccessToken('tok');
      expect(service.hasAccessToken()).toBe(true);
    });

    it('hasAccessToken() returns false when absent', () => {
      expect(service.hasAccessToken()).toBe(false);
    });
  });

  describe('refresh token', () => {
    it('saves and retrieves a refresh token', () => {
      service.saveRefreshToken('refresh-xyz');
      expect(service.getRefreshToken()).toBe('refresh-xyz');
    });

    it('returns null when no refresh token stored', () => {
      expect(service.getRefreshToken()).toBeNull();
    });

    it('removes the refresh token', () => {
      service.saveRefreshToken('refresh-xyz');
      service.removeRefreshToken();
      expect(service.getRefreshToken()).toBeNull();
    });
  });

  describe('clearTokens()', () => {
    it('removes both access and refresh tokens', () => {
      service.saveAccessToken('a');
      service.saveRefreshToken('r');
      service.clearTokens();
      expect(service.getAccessToken()).toBeNull();
      expect(service.getRefreshToken()).toBeNull();
    });
  });

  describe('storage key config', () => {
    it('uses configured accessTokenKey', () => {
      service.saveAccessToken('t1');
      expect(localStorage.getItem('test_access_token')).toBe('t1');
    });

    it('uses configured refreshTokenKey', () => {
      service.saveRefreshToken('t2');
      expect(localStorage.getItem('test_refresh_token')).toBe('t2');
    });
  });
});
