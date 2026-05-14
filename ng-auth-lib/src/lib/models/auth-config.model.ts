export interface AuthConfig {
  /** Backend base URL, e.g. http://localhost:8080 */
  apiEndpoint: string;

  /** Full login URL (default: apiEndpoint + /api/auth/login) */
  loginUrl?: string;

  /** Full refresh URL (default: apiEndpoint + /api/auth/refresh) */
  refreshUrl?: string;

  /** Full logout URL (default: apiEndpoint + /api/auth/logout) */
  logoutUrl?: string;

  /** Full validate URL (default: apiEndpoint + /api/auth/validate) */
  validateUrl?: string;

  /** localStorage key for the access token (default: 'access_token') */
  accessTokenKey?: string;

  /** localStorage key for the refresh token (default: 'refresh_token') */
  refreshTokenKey?: string;

  /**
   * URL patterns that should receive the Authorization header.
   * When provided, only matching URLs get the JWT attached.
   * When empty (default), all requests to apiEndpoint get the header.
   */
  whitelistedUrls?: string[];

  /**
   * URL patterns that must NEVER receive the Authorization header
   * (e.g. login, refresh endpoints).
   */
  blacklistedUrls?: string[];

  /** Route to redirect to after successful login (default: '/') */
  defaultRedirectUrl?: string;

  /** Route to redirect to when unauthenticated (default: '/login') */
  loginRedirectUrl?: string;
}

/** Resolved config with all defaults applied — used internally. */
export interface ResolvedAuthConfig extends Required<AuthConfig> {}
