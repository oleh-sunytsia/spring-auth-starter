package com.benatti.auth.user;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * Bridges Spring Security's UserDetailsService with the library's
 * UserDetailsProvider, so Spring Security's AuthenticationManager
 * can use the developer-supplied provider without modification.
 */
public class UserDetailsServiceAdapter implements UserDetailsService {

    private final UserDetailsProvider delegate;

    public UserDetailsServiceAdapter(UserDetailsProvider delegate) {
        this.delegate = delegate;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return delegate.loadUserByUsername(username);
    }
}
