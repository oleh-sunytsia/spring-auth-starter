package com.benatti.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Admin-only endpoints.
 * Access: requires ROLE_ADMIN (@PreAuthorize from Spring Security method security).
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    /**
     * GET /api/admin/stats
     * Returns fake application statistics (admin only).
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(Map.of(
                "totalUsers",        2,
                "activeSessionsEst", 1,
                "serverTime",        Instant.now().toString(),
                "message",           "Welcome, Admin!"
        ));
    }

    /**
     * GET /api/admin/users
     * Returns a simulated list of users (admin only).
     */
    @GetMapping("/users")
    public ResponseEntity<Object> users() {
        return ResponseEntity.ok(java.util.List.of(
                Map.of("userId", "uid-alice", "username", "alice",
                       "email", "alice@demo.com", "roles", java.util.List.of("USER")),
                Map.of("userId", "uid-bob",   "username", "bob",
                       "email", "bob@demo.com",
                       "roles", java.util.List.of("USER", "ADMIN"))
        ));
    }
}
