package com.benatti.auth.security;

import com.benatti.auth.jwt.JwtException;
import com.benatti.auth.jwt.JwtPayload;
import com.benatti.auth.jwt.JwtTokenStore;
import com.benatti.auth.user.AuthUserDetails;
import com.benatti.auth.user.UserDetailsProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Intercepts every HTTP request, extracts the JWT Bearer token,
 * validates it, and populates the Spring Security context.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX        = "Bearer ";

    private final JwtTokenStore       jwtTokenStore;
    private final UserDetailsProvider userDetailsProvider;

    public JwtAuthenticationFilter(JwtTokenStore jwtTokenStore,
                                   UserDetailsProvider userDetailsProvider) {
        this.jwtTokenStore       = jwtTokenStore;
        this.userDetailsProvider = userDetailsProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest  request,
                                    HttpServletResponse response,
                                    FilterChain         filterChain)
            throws ServletException, IOException {

        String token = extractToken(request);

        if (StringUtils.hasText(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                JwtPayload payload = jwtTokenStore.validateToken(token);

                // Only access tokens are accepted for authentication
                if (!"access".equals(payload.getType())) {
                    filterChain.doFilter(request, response);
                    return;
                }

                AuthUserDetails userDetails = userDetailsProvider.loadUserById(payload.getSubject());

                JwtAuthenticationToken authentication =
                        new JwtAuthenticationToken(userDetails, token,
                                userDetails.getAuthorities());
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (JwtException | UserDetailsProvider.UserNotFoundException ignored) {
                // Invalid/expired token → continue without auth; Spring Security will
                // return 401 for protected endpoints.
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
