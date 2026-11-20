package com.dijo.security;

import com.dijo.user.Role;

import java.util.Arrays;

/**
 * The login page a credential submission came from. Each portal only accepts
 * users with its own role (UC-001 BR-01, UC-002 BR-01).
 */
public enum LoginPortal {

    ADMIN("admin", Role.ADMIN, AdminLoginView.ROUTE),
    CLIENT("client", Role.CLIENT, ClientLoginView.ROUTE);

    /** Request parameter carried on the login form's action URL. */
    public static final String PARAMETER = "portal";

    private final String id;
    private final Role role;
    private final String loginRoute;

    LoginPortal(String id, Role role, String loginRoute) {
        this.id = id;
        this.role = role;
        this.loginRoute = loginRoute;
    }

    public Role role() {
        return role;
    }

    public String loginRoute() {
        return loginRoute;
    }

    /** Form action for this portal's login form (posts to Spring Security's login processing URL). */
    public String formAction() {
        return ClientLoginView.ROUTE + "?" + PARAMETER + "=" + id;
    }

    /** Resolves the portal from a request parameter; anything unknown is treated as the client portal. */
    public static LoginPortal fromParameter(String value) {
        return Arrays.stream(values())
                .filter(p -> p.id.equals(value))
                .findFirst()
                .orElse(CLIENT);
    }
}
