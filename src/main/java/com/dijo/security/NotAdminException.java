package com.dijo.security;

import org.springframework.security.core.AuthenticationException;

/**
 * Thrown when valid credentials belong to a user without the ADMIN role
 * (UC-001 AF-2).
 */
public class NotAdminException extends AuthenticationException {

    public NotAdminException() {
        super("Access denied. Admin privileges required.");
    }
}
