package com.benatti.auth.event;

/**
 * Published when a user explicitly logs out (refresh token revoked).
 */
public class LogoutEvent extends AuthenticationEvent {

    public LogoutEvent(Object source, String userId) {
        super(source, userId);
    }
}
