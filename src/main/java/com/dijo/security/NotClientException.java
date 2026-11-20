package com.dijo.security;

import org.springframework.security.core.AuthenticationException;

/**
 * Thrown when valid credentials entered on the client login page belong to a
 * user without the CLIENT role (UC-002 AF-2).
 */
public class NotClientException extends AuthenticationException {

    public NotClientException() {
        super("Access denied. This account is for admin use only.");
    }
}
