export interface AuthToken {
  accessToken: string;
  refreshToken: string;
  /** Expiration in seconds */
  expiresIn: number;
}
