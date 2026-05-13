package com.benatti.auth.event;

/**
 * Published after a user successfully authenticates.
 *
 * <pre>{@code
 * @EventListener
 * public void onLogin(LoginSuccessEvent event) {
 *     auditLog.record(event.getUserId(), event.getUsername(), "LOGIN_SUCCESS");
 * }
 * }</pre>
 */
public class LoginSuccessEvent extends AuthenticationEvent {

    private final String username;

    public LoginSuccessEvent(Object source, String userId, String username) {
        super(source, userId);
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}
