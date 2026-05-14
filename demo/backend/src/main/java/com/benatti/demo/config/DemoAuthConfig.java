package com.benatti.demo.config;

import com.benatti.auth.user.AuthUserDetails;
import com.benatti.auth.user.DefaultAuthUserDetails;
import com.benatti.auth.user.UserDetailsProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.benatti.auth.user.UserDetailsProvider.UserNotFoundException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Demo configuration: wires a hardcoded in-memory user store.
 * In a real application this would load users from a JPA repository.
 */
@Slf4j
@Configuration
public class DemoAuthConfig {

    /**
     * Registers a custom {@link UserDetailsProvider} bean.
     * Because this bean is present, {@code benatti-auth-starter} will use it
     * instead of throwing "no UserDetailsProvider found".
     */
    @Bean
    public UserDetailsProvider userDetailsProvider(PasswordEncoder passwordEncoder) {
        // Build a small in-memory user registry once at startup
        Map<String, AuthUserDetails> byUsername = new ConcurrentHashMap<>();
        Map<String, AuthUserDetails> byId       = new ConcurrentHashMap<>();

        AuthUserDetails alice = DefaultAuthUserDetails.builder()
                .userId("uid-alice")
                .username("alice")
                .password(passwordEncoder.encode("password"))
                .email("alice@demo.com")
                .roles(List.of("USER"))
                .permissions(List.of("READ:PROFILE"))
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .lastLogin(Instant.now())
                .build();

        AuthUserDetails bob = DefaultAuthUserDetails.builder()
                .userId("uid-bob")
                .username("bob")
                .password(passwordEncoder.encode("password"))
                .email("bob@demo.com")
                .roles(List.of("USER", "ADMIN"))
                .permissions(List.of("READ:PROFILE", "READ:USERS", "WRITE:USERS"))
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .lastLogin(Instant.now())
                .build();

        byUsername.put(alice.getUsername(), alice);
        byUsername.put(bob.getUsername(),   bob);
        byId.put(alice.getUserId(), alice);
        byId.put(bob.getUserId(),   bob);

        log.info("Demo users registered: {}", byUsername.keySet());

        return new UserDetailsProvider() {
            @Override
            public AuthUserDetails loadUserByUsername(String username) {
                AuthUserDetails user = byUsername.get(username);
                if (user == null) {
                    throw new UsernameNotFoundException("User not found: " + username);
                }
                return user;
            }

            @Override
            public AuthUserDetails loadUserById(String userId) {
                AuthUserDetails user = byId.get(userId);
                if (user == null) {
                    throw new UserNotFoundException("User not found by id: " + userId);
                }
                return user;
            }
        };
    }
}
