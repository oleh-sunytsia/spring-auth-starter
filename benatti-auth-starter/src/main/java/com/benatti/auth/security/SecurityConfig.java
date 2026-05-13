package com.benatti.auth.security;

import com.benatti.auth.autoconfigure.AuthProperties;
import com.benatti.auth.user.UserDetailsServiceAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final AuthProperties            properties;
    private final JwtAuthenticationFilter   jwtFilter;
    private final UserDetailsServiceAdapter userDetailsServiceAdapter;
    private final PasswordEncoder           passwordEncoder;

    public SecurityConfig(AuthProperties properties,
                          JwtAuthenticationFilter jwtFilter,
                          UserDetailsServiceAdapter userDetailsServiceAdapter,
                          PasswordEncoder passwordEncoder) {
        this.properties               = properties;
        this.jwtFilter                = jwtFilter;
        this.userDetailsServiceAdapter = userDetailsServiceAdapter;
        this.passwordEncoder          = passwordEncoder;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        String base = properties.getBasePath();

        List<String> publicPatterns = buildPublicPatterns(base);

        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> {
                auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                publicPatterns.forEach(p -> auth.requestMatchers(p).permitAll());
                properties.getPublicPaths().forEach(p -> auth.requestMatchers(p).permitAll());
                auth.anyRequest().authenticated();
            })
            .authenticationProvider(daoAuthProvider())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider daoAuthProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsServiceAdapter);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(properties.getCorsAllowedOrigins());
        config.setAllowedMethods(properties.getCorsAllowedMethods());
        config.setAllowedHeaders(properties.getCorsAllowedHeaders());
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private List<String> buildPublicPatterns(String base) {
        List<String> list = new ArrayList<>();
        list.add(base + "/login");
        list.add(base + "/refresh");
        list.add(base + "/logout");
        list.add(base + "/validate");
        return list;
    }
}
