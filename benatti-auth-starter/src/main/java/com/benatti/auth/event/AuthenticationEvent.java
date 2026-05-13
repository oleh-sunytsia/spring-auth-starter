package com.benatti.auth.event;

import org.springframework.context.ApplicationEvent;

/**
 * Base class for all authentication-related domain events.
 * Developers can listen with @EventListener for auditing, metrics, etc.
 */
public abstract class AuthenticationEvent extends ApplicationEvent {

    private final String userId;

    protected AuthenticationEvent(Object source, String userId) {
        super(source);
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }
}
