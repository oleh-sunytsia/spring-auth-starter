import { TokenDecoderService } from './token-decoder.service';

// ── Helpers ───────────────────────────────────────────────────────────────

/**
 * Build a minimal unsigned JWT with the given payload.
 * Signature is fake — TokenDecoderService never verifies it.
 */
function makeJwt(payload: Record<string, unknown>): string {
  const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }))
    .replace(/\+/g, '-').replace(/\//g, '_').replace(/=/g, '');
  const body = btoa(JSON.stringify(payload))
    .replace(/\+/g, '-').replace(/\//g, '_').replace(/=/g, '');
  return `${header}.${body}.fakesignature`;
}

function futureExp(secondsFromNow: number): number {
  return Math.floor(Date.now() / 1000) + secondsFromNow;
}

function pastExp(secondsAgo: number): number {
  return Math.floor(Date.now() / 1000) - secondsAgo;
}

// ── Tests ─────────────────────────────────────────────────────────────────

describe('TokenDecoderService', () => {
  let service: TokenDecoderService;

  beforeEach(() => {
    service = new TokenDecoderService();
  });

  describe('decodePayload()', () => {
    it('returns payload object for a valid JWT', () => {
      const token = makeJwt({ sub: 'user1', username: 'alice' });
      const result = service.decodePayload(token);
      expect(result).not.toBeNull();
      expect(result!.sub).toBe('user1');
      expect(result!.username).toBe('alice');
    });

    it('returns null for a non-JWT string', () => {
      expect(service.decodePayload('not-a-jwt')).toBeNull();
    });

    it('returns null for an empty string', () => {
      expect(service.decodePayload('')).toBeNull();
    });

    it('returns null for a two-part string', () => {
      expect(service.decodePayload('header.body')).toBeNull();
    });

    it('returns null when payload section is invalid base64', () => {
      expect(service.decodePayload('header.!!!.sig')).toBeNull();
    });
  });

  describe('isExpired()', () => {
    it('returns false when exp is in the future', () => {
      const token = makeJwt({ exp: futureExp(3600) });
      expect(service.isExpired(token)).toBe(false);
    });

    it('returns true when exp is in the past', () => {
      const token = makeJwt({ exp: pastExp(10) });
      expect(service.isExpired(token)).toBe(true);
    });

    it('returns true when exp claim is absent', () => {
      const token = makeJwt({ sub: 'user1' });
      expect(service.isExpired(token)).toBe(true);
    });

    it('returns true for an invalid token', () => {
      expect(service.isExpired('garbage')).toBe(true);
    });
  });

  describe('secondsUntilExpiry()', () => {
    it('returns a positive number for a future token', () => {
      const token = makeJwt({ exp: futureExp(120) });
      const secs = service.secondsUntilExpiry(token);
      expect(secs).toBeGreaterThan(0);
      expect(secs).toBeLessThanOrEqual(120);
    });

    it('returns 0 for an already-expired token', () => {
      const token = makeJwt({ exp: pastExp(60) });
      expect(service.secondsUntilExpiry(token)).toBe(0);
    });

    it('returns 0 for an invalid token', () => {
      expect(service.secondsUntilExpiry('bad')).toBe(0);
    });
  });

  describe('getRoles()', () => {
    it('returns roles array from payload', () => {
      const token = makeJwt({ roles: ['ROLE_ADMIN', 'ROLE_USER'] });
      expect(service.getRoles(token)).toEqual(['ROLE_ADMIN', 'ROLE_USER']);
    });

    it('returns empty array when roles absent', () => {
      const token = makeJwt({ sub: 'u1' });
      expect(service.getRoles(token)).toEqual([]);
    });
  });

  describe('getPermissions()', () => {
    it('returns permissions array from payload', () => {
      const token = makeJwt({ permissions: ['READ:USERS', 'WRITE:ORDERS'] });
      expect(service.getPermissions(token)).toEqual(['READ:USERS', 'WRITE:ORDERS']);
    });

    it('returns empty array when permissions absent', () => {
      const token = makeJwt({ sub: 'u1' });
      expect(service.getPermissions(token)).toEqual([]);
    });
  });
});
