package com.benatti.auth.event;

/**
 * Published when the access token is successfully refreshed.
 */
public class TokenRefreshEvent extends AuthenticationEvent {

    public TokenRefreshEvent(Object source, String userId) {
        super(source, userId);
    }
}
