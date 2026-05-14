package com.benatti.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Demo application that showcases how to integrate benatti-auth-starter.
 *
 * Prerequisites:
 *   cd ../benatti-auth-starter && mvn install -DskipTests
 *
 * Run:
 *   mvn spring-boot:run
 *
 * Sample accounts (defined in DemoAuthConfig):
 *   alice / password  → roles: USER
 *   bob   / password  → roles: USER, ADMIN
 *
 * Auth endpoints (auto-registered by the starter):
 *   POST /api/auth/login
 *   POST /api/auth/refresh
 *   POST /api/auth/logout
 *   GET  /api/auth/validate
 *
 * Demo endpoints:
 *   GET  /api/users/me        (any authenticated user)
 *   GET  /api/admin/stats     (ADMIN only)
 */
@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
