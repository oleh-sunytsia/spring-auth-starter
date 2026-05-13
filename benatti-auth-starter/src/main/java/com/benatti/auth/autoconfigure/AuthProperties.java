package com.benatti.auth.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "auth")
public class AuthProperties {

    /** Enable/disable the starter */
    private boolean enabled = true;

    /** JWT secret key (min 32 chars, HMAC-SHA256) */
    private String jwtSecret = "benatti-default-secret-key-min-32-chars!";

    /** Access token expiration in minutes (default 60) */
    private long accessTokenExpirationMinutes = 60;

    /** Refresh token expiration in days (default 30) */
    private long refreshTokenExpirationDays = 30;

    /** JWT issuer claim */
    private String tokenIssuer = "benatti-auth";

    /** Base path for auth endpoints */
    private String basePath = "/api/auth";

    /** Permit-all URL patterns (in addition to auth endpoints) */
    private List<String> publicPaths = new ArrayList<>();

    /** CORS allowed origins */
    private List<String> corsAllowedOrigins = List.of("*");

    /** CORS allowed methods */
    private List<String> corsAllowedMethods = List.of("GET", "POST", "PUT", "DELETE", "OPTIONS");

    /** CORS allowed headers */
    private List<String> corsAllowedHeaders = List.of("*");

    /** Use in-memory refresh token store (true) or expect JPA bean (false) */
    private boolean inMemoryTokenStore = true;

    // ── getters & setters ──────────────────────────────────────────────────

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getJwtSecret() { return jwtSecret; }
    public void setJwtSecret(String jwtSecret) { this.jwtSecret = jwtSecret; }

    public long getAccessTokenExpirationMinutes() { return accessTokenExpirationMinutes; }
    public void setAccessTokenExpirationMinutes(long accessTokenExpirationMinutes) {
        this.accessTokenExpirationMinutes = accessTokenExpirationMinutes;
    }

    public long getRefreshTokenExpirationDays() { return refreshTokenExpirationDays; }
    public void setRefreshTokenExpirationDays(long refreshTokenExpirationDays) {
        this.refreshTokenExpirationDays = refreshTokenExpirationDays;
    }

    public String getTokenIssuer() { return tokenIssuer; }
    public void setTokenIssuer(String tokenIssuer) { this.tokenIssuer = tokenIssuer; }

    public String getBasePath() { return basePath; }
    public void setBasePath(String basePath) { this.basePath = basePath; }

    public List<String> getPublicPaths() { return publicPaths; }
    public void setPublicPaths(List<String> publicPaths) { this.publicPaths = publicPaths; }

    public List<String> getCorsAllowedOrigins() { return corsAllowedOrigins; }
    public void setCorsAllowedOrigins(List<String> corsAllowedOrigins) {
        this.corsAllowedOrigins = corsAllowedOrigins;
    }

    public List<String> getCorsAllowedMethods() { return corsAllowedMethods; }
    public void setCorsAllowedMethods(List<String> corsAllowedMethods) {
        this.corsAllowedMethods = corsAllowedMethods;
    }

    public List<String> getCorsAllowedHeaders() { return corsAllowedHeaders; }
    public void setCorsAllowedHeaders(List<String> corsAllowedHeaders) {
        this.corsAllowedHeaders = corsAllowedHeaders;
    }

    public boolean isInMemoryTokenStore() { return inMemoryTokenStore; }
    public void setInMemoryTokenStore(boolean inMemoryTokenStore) {
        this.inMemoryTokenStore = inMemoryTokenStore;
    }
}
