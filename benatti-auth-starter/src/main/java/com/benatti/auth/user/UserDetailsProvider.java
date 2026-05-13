package com.benatti.auth.user;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public interface UserDetailsProvider extends UserDetailsService {
    
    /**
     * Load user by username (for authentication)
     *
     * @param username username
     * @return user details
     * @throws UsernameNotFoundException if user not found
     */
    @Override
    AuthUserDetails loadUserByUsername(String username) throws UsernameNotFoundException;
    
    /**
     * Load user by ID (for token validation)
     *
     * @param userId user ID
     * @return user details
     * @throws UserNotFoundException if user not found
     */
    AuthUserDetails loadUserById(String userId) throws UserNotFoundException;
    
    /**
     * Exception for unknown user by ID
     */
    class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(String message) {
            super(message);
        }
    }
}
