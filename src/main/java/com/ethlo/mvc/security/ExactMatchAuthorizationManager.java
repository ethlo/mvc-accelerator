package com.ethlo.mvc.security;

import org.springframework.lang.Nullable;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class ExactMatchAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final Set<String> allowed;

    public ExactMatchAuthorizationManager(List<String> allowed) {
        this.allowed = new HashSet<>(allowed);
    }

    @Nullable
    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext context) {
        final String path = context.getRequest().getServletPath();
        if (allowed.contains(path)) {
            return new AuthorizationDecision(true);
        }
        return null;
    }
}