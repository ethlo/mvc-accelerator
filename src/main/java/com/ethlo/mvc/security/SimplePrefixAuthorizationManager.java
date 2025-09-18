package com.ethlo.mvc.security;

import org.springframework.lang.Nullable;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.util.List;
import java.util.function.Supplier;

public class SimplePrefixAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final List<String> allowed;

    public SimplePrefixAuthorizationManager(List<String> allowed) {
        this.allowed = allowed;
    }

    @Nullable
    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext context) {
        final String path = context.getRequest().getServletPath();
        if (allowed.stream().anyMatch(path::startsWith)) {
            return new AuthorizationDecision(true);
        }
        return null;
    }
}