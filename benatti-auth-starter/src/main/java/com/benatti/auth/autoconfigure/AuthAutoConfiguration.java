package com.benatti.auth.autoconfigure;

import com.benatti.auth.auth.AuthService;
import com.benatti.auth.auth.DefaultAuthService;
import com.benatti.auth.controller.AuthController;
import com.benatti.auth.exception.GlobalExceptionHandler;
import com.benatti.auth.jwt.HmacJwtProvider;
import com.benatti.auth.jwt.JwtProvider;
import com.benatti.auth.jwt.JwtTokenStore;
import com.benatti.auth.security.JwtAuthenticationFilter;
import com.benatti.auth.security.SecurityConfig;
import com.benatti.auth.storage.InMemoryRefreshTokenStore;
import com.benatti.auth.storage.RefreshTokenRepository;
import com.benatti.auth.user.DefaultAuthUserDetails;
import com.benatti.auth.user.UserDetailsProvider;
import com.benatti.auth.user.UserDetailsServiceAdapter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@AutoConfiguration
@ConditionalOnProperty(name = "auth.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(AuthProperties.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
public class AuthAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JwtProvider jwtProvider(AuthProperties properties) {
        return new HmacJwtProvider(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtTokenStore jwtTokenStore(JwtProvider jwtProvider, AuthProperties properties) {
        return new JwtTokenStore(jwtProvider, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "auth.in-memory-token-store", havingValue = "true", matchIfMissing = true)
    public RefreshTokenRepository refreshTokenRepository() {
        return new InMemoryRefreshTokenStore();
    }

    @Bean
    @ConditionalOnMissingBean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @ConditionalOnMissingBean
    public UserDetailsServiceAdapter userDetailsServiceAdapter(UserDetailsProvider userDetailsProvider) {
        return new UserDetailsServiceAdapter(userDetailsProvider);
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenStore jwtTokenStore,
                                                           UserDetailsProvider userDetailsProvider) {
        return new JwtAuthenticationFilter(jwtTokenStore, userDetailsProvider);
    }

    @Bean
    @ConditionalOnMissingBean(AuthService.class)
    public AuthService authService(AuthenticationManager authenticationManager,
                                   UserDetailsProvider userDetailsProvider,
                                   JwtTokenStore jwtTokenStore,
                                   RefreshTokenRepository refreshTokenRepository,
                                   ApplicationEventPublisher eventPublisher,
                                   AuthProperties properties) {
        return new DefaultAuthService(authenticationManager, userDetailsProvider,
                jwtTokenStore, refreshTokenRepository, eventPublisher, properties);
    }

    @Bean
    public AuthController authController(AuthService authService, AuthProperties properties) {
        return new AuthController(authService, properties);
    }
}
