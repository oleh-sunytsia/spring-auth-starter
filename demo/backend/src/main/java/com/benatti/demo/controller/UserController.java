package com.benatti.demo.controller;

import com.benatti.auth.user.AuthUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Protected API endpoints available to all authenticated users.
 *
 * Access: any authenticated user (Bearer token required).
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    /**
     * GET /api/users/me
     * Returns the profile of the currently authenticated user.
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(
            @AuthenticationPrincipal AuthUserDetails principal) {

        return ResponseEntity.ok(Map.of(
                "userId",      principal.getUserId(),
                "username",    principal.getUsername(),
                "email",       principal.getEmail(),
                "roles",       principal.getAuthorities().stream()
                                       .map(a -> a.getAuthority()).toList(),
                "permissions", principal.getPermissions()
        ));
    }

    /**
     * GET /api/users/token-info
     * Returns a brief description of the claims in the current token.
     */
    @GetMapping("/token-info")
    public ResponseEntity<Map<String, Object>> tokenInfo(
            @AuthenticationPrincipal AuthUserDetails principal) {

        return ResponseEntity.ok(Map.of(
                "userId",    principal.getUserId(),
                "username",  principal.getUsername(),
                "lastLogin", principal.getLastLogin() != null
                             ? principal.getLastLogin().toString()
                             : "N/A"
        ));
    }
}
