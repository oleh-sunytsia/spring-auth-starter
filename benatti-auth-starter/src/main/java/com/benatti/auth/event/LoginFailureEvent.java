package com.benatti.auth.event;

/**
 * Published when a login attempt fails (bad credentials, locked account, etc.).
 *
 * <pre>{@code
 * @EventListener
 * public void onFailure(LoginFailureEvent event) {
 *     rateLimiter.record(event.getUsername());
 * }
 * }</pre>
 */
public class LoginFailureEvent extends AuthenticationEvent {

    private final String username;
    private final String reason;

    public LoginFailureEvent(Object source, String username, String reason) {
        super(source, null);   // userId unknown on failure
        this.username = username;
        this.reason   = reason;
    }

    public String getUsername() { return username; }
    public String getReason()   { return reason; }
}
